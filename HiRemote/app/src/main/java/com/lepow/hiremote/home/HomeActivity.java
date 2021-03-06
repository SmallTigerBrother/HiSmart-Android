package com.lepow.hiremote.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.camera.Camera;
import com.lepow.hiremote.R;
import com.lepow.hiremote.app.BaseActivity;
import com.lepow.hiremote.authorise.LoginActivity;
import com.lepow.hiremote.bluetooth.HSBLEPeripheralManager;
import com.lepow.hiremote.bluetooth.data.PeripheralDataManager;
import com.lepow.hiremote.bluetooth.data.PeripheralInfo;
import com.lepow.hiremote.home.widget.PeripheralStatusView;
import com.lepow.hiremote.lbs.FindMyItemActivity;
import com.lepow.hiremote.lbs.PinnedLocationHistoryActivity;
import com.lepow.hiremote.lbs.data.LocationDataManager;
import com.lepow.hiremote.lbs.data.LocationInfo;
import com.lepow.hiremote.misc.IntentAction;
import com.lepow.hiremote.misc.IntentKeys;
import com.lepow.hiremote.misc.ServerUrls;
import com.lepow.hiremote.notification.NotificationManager;
import com.lepow.hiremote.record.VoiceMemosActivity;
import com.lepow.hiremote.setting.AppSettings;
import com.lepow.hiremote.setting.SettingActivity;
import com.lepow.hiremote.widget.ProgressDialog;
import com.mn.tiger.app.TGApplicationProxy;
import com.mn.tiger.bluetooth.TGBLEManager;
import com.mn.tiger.bluetooth.data.TGBLEPeripheralInfo;
import com.mn.tiger.location.TGLocation;
import com.mn.tiger.location.TGLocationManager;
import com.mn.tiger.log.Logger;
import com.mn.tiger.notification.TGNotificationBuilder;
import com.mn.tiger.upgrade.TGUpgradeManager;
import com.mn.tiger.utility.DisplayUtils;
import com.mn.tiger.widget.TGNavigationBar;
import com.mn.tiger.widget.viewpager.DotIndicatorBannerPagerView;
import com.mn.tiger.widget.viewpager.DotIndicatorBannerPagerView.ViewPagerHolder;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

/**
 * 主界面
 */
public class HomeActivity extends BaseActivity implements View.OnClickListener, ViewPager.OnPageChangeListener
{
	private static final Logger LOG = Logger.getLogger(HomeActivity.class);

	@Bind(R.id.devices_viewpager)
	DotIndicatorBannerPagerView<PeripheralInfo> bannerPagerView;

	@Bind(R.id.common_function_btn)
	Button functionBtn;

	@Bind(R.id.common_function_bord)
	LinearLayout functionBord;

	@Bind(R.id.function_pinned_location_image)
	Button pinnedLocationImg;

	@Bind(R.id.common_settings_btn)
	Button settingsBtn;

	@Bind(R.id.common_settings_bord)
	LinearLayout settingsBord;

	@Bind(R.id.notification_switch)
	Switch notificationSwitch;

	@Bind(R.id.voice_switch)
	Switch voiceSwitch;

	@Bind(R.id.play_sound_switch)
	Switch playSoundSwitch;

	private PeripheralInfo connectedPeripheral;

	private Handler handler = new Handler();

	private List<PeripheralInfo> peripheralInfos;

	private ProgressDialog progressDialog;

	private int currentPeripheralIndex = 0;

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(intent.getAction().equals(IntentAction.ACTION_READ_PERIPHERAL_POWER))
			{
				onPeripheralChanged();
			}
			else if(intent.getAction().equals(IntentAction.ACTION_READ_DISCONNECTED_ALARM))
			{
				playSoundSwitch.setChecked(HSBLEPeripheralManager.getInstance().isDisconnectedAlarmEnable());
			}
			else if(intent.getAction().equals(TGBLEManager.ACTION_BLE_STATE_CHANGE))
			{
				int bleState = TGBLEManager.getBLEState(intent);
				switch (bleState)
				{
					case TGBLEManager.BLE_STATE_CONNECTED:
						onPeripheralChanged();
						break;
					case TGBLEManager.BLE_STATE_DISCONNECTED:
						connectedPeripheral = PeripheralInfo.NULL_OBJECT;
						onPeripheralChanged();
						showDisconnectedNotification();
						LOG.d("[Method:onReceive] disconnected peripheral ");
                        progressDialog.dismiss();
						break;

					case TGBLEManager.BLE_STATE_NONSUPPORT:
						HSBLEPeripheralManager.getInstance().showBluetoothOffDialog(HomeActivity.this);
                        progressDialog.dismiss();
						break;

					case TGBLEManager.BLE_STATE_NO_PERIPHERAL_FOUND:
						connectedPeripheral = PeripheralInfo.NULL_OBJECT;
						LOG.d("[Method:onReceive] not found peripheral ");
                        progressDialog.dismiss();
                        onPeripheralChanged();
						break;

					default:
						break;
				}
			}
		}

		private void showDisconnectedNotification()
		{
			boolean pushNotificationSettingOn = AppSettings.isPushNotificationSettingOn(HomeActivity.this);
			LOG.d("[Method:showDisconnectedNotification] pushNotificationSettingOn == " + pushNotificationSettingOn);
			if(pushNotificationSettingOn)
			{
				TGNotificationBuilder builder = new TGNotificationBuilder(HomeActivity.this);
				builder.setContentTitle(getString(R.string.oops));
				builder.setContentText(getString(R.string.your_device_far_away_from_you));
				builder.setSmallIcon(R.drawable.ic_launcher);
				builder.setClass(HomeActivity.class);
				NotificationManager.getInstanse().showNotification(HomeActivity.this, 0, builder);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		progressDialog = new ProgressDialog(this);
		progressDialog.show();

		getWindow().getDecorView().setBackgroundResource(R.drawable.home_page_bg);
		setBarTitleText(getString(R.string.app_name));

		ButterKnife.bind(this);

		PeripheralInfo peripheralInfo = (PeripheralInfo)getIntent().getSerializableExtra(IntentKeys.PERIPHERAL_INFO);
		connectedPeripheral = null != peripheralInfo ? peripheralInfo : PeripheralInfo.NULL_OBJECT;

		initViews();

		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				//检测更新
				LOG.d("check upgrade");
				TGUpgradeManager.upgrade(ServerUrls.CHECK_UPGRADE_URL);
				registerReceiver(broadcastReceiver, new IntentFilter(IntentAction.ACTION_READ_PERIPHERAL_POWER));
				registerReceiver(broadcastReceiver, new IntentFilter(IntentAction.ACTION_READ_DISCONNECTED_ALARM));
				registerReceiver(broadcastReceiver, new IntentFilter(TGBLEManager.ACTION_BLE_STATE_CHANGE));

				//读取蓝牙设备特征值
				HSBLEPeripheralManager.getInstance().readAllCharacteristics();
			}
		}, 1000);

		//启动守护线程，当没有任何设备连接时，每隔1分钟进行设备扫描
		HSBLEPeripheralManager.getInstance().startDeamonThread();
	}

	@Override
	protected void initNavigationResource(TGNavigationBar navigationBar)
	{
		super.initNavigationResource(navigationBar);
		navigationBar.setBackgroundColor(Color.TRANSPARENT);
		navigationBar.getMiddleTextView().setTextColor(Color.WHITE);
	}

	private void initViews()
	{
		showRightBarButton(true);
		getRightBarButton().setImageResource(R.drawable.navi_setting);
		getRightBarButton().setOnClickListener(this);

		showLeftBarButton(false);
		getLeftBarButton().setImageResource(R.drawable.add_device);
		getLeftBarButton().setOnClickListener(this);

		initBannerIndicator();
		bannerPagerView.setCircleable(false);
		bannerPagerView.setViewPagerHolder(new ViewPagerHolder<PeripheralInfo>()
		{
			@Override
			public View initViewOfPage(Activity activity, PagerAdapter adapter, int viewType)
			{
				return new PeripheralStatusView(activity);
			}

			@Override
			public void fillData(View viewOfPage, final PeripheralInfo itemData, int position, int viewType)
			{
				((PeripheralStatusView) viewOfPage).setData(itemData);
				viewOfPage.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						//点击图标时，连接未连接的设备
						if (!itemData.isConnected())
						{
							handler.postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									HSBLEPeripheralManager.getInstance().scanTargetPeripheral(itemData.getMacAddress());
								}
							}, 2000);

							progressDialog.show();
						}
					}
				});
			}
		});

		bannerPagerView.setOnPageChangeListener(this);

		showFunctionBoard();

		initSettingsBoard();

		onPeripheralChanged();

		//保存定位记录
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				TGLocation lastLocation = TGLocationManager.getInstance().getLastLocation();
				if(null != lastLocation)
				{
					LocationInfo locationInfo = LocationInfo.fromLocation(lastLocation);
					LocationDataManager.getInstance().savePinnedLocation(TGApplicationProxy.getInstance().getApplication(), locationInfo);
				}
			}
		}, 8000);
	}

	private void initBannerIndicator()
	{
		GradientDrawable dotDefaultShapeDrawable = new GradientDrawable();
		dotDefaultShapeDrawable.setShape(GradientDrawable.OVAL);
		dotDefaultShapeDrawable.setColor(0x80ffffff);
		dotDefaultShapeDrawable.setSize(DisplayUtils.dip2px(this, 6),
				DisplayUtils.dip2px(this, 6));

		GradientDrawable dotSelectedShapeDrawable = new GradientDrawable();
		dotSelectedShapeDrawable.setShape(GradientDrawable.OVAL);
		dotSelectedShapeDrawable.setSize(DisplayUtils.dip2px(this, 6),
				DisplayUtils.dip2px(this, 6));
		dotSelectedShapeDrawable.setColor(0xffffffff);

		bannerPagerView.setDotViewBackground(dotDefaultShapeDrawable, dotSelectedShapeDrawable);
	}

	private void onPeripheralChanged()
	{
		TGBLEPeripheralInfo tgblePeripheralInfo = HSBLEPeripheralManager.getInstance().getCurrentPeripheral();
		if(null != tgblePeripheralInfo)
		{
			connectedPeripheral = PeripheralInfo.fromBLEPeripheralInfo(tgblePeripheralInfo);
			connectedPeripheral.setConnected(true);
			connectedPeripheral.setEnergy(HSBLEPeripheralManager.getInstance().getPeripheralPower());
			PeripheralDataManager.savePeripheral(this, connectedPeripheral);
		}
		else
		{
			connectedPeripheral = PeripheralInfo.NULL_OBJECT;
		}

		PeripheralInfo lastPeripheral = PeripheralInfo.fromBLEPeripheralInfo(HSBLEPeripheralManager.getInstance().getLastPeripheral());

		playSoundSwitch.setChecked(HSBLEPeripheralManager.getInstance().isDisconnectedAlarmEnable());
		peripheralInfos = PeripheralDataManager.getAllPeripherals(this, connectedPeripheral, lastPeripheral);
		bannerPagerView.setData(peripheralInfos);
		if(!connectedPeripheral.equals(PeripheralInfo.NULL_OBJECT))
		{
			currentPeripheralIndex = peripheralInfos.indexOf(connectedPeripheral);
			bannerPagerView.setCurrentPage(currentPeripheralIndex);
		}
		else
		{
			bannerPagerView.setCurrentPage(currentPeripheralIndex);
		}

		//无设备连接或者设备电量大于0时，隐藏进度框
		if(connectedPeripheral.equals(PeripheralInfo.NULL_OBJECT) || connectedPeripheral.getEnergy() > 0)
		{
			progressDialog.dismiss();
		}

		LOG.d("[Method:onPeripheralChanged]  currentPeripheralIndex == " + currentPeripheralIndex);
	}

	private void initSettingsBoard()
	{
		notificationSwitch.setChecked(AppSettings.isPushNotificationSettingOn(this));
		voiceSwitch.setChecked(AppSettings.getMode() == AppSettings.MODE_RECORD);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
	{
	}

	@Override
	public void onPageScrollStateChanged(int state)
	{
	}

	public void onPageSelected(int position)
	{
		if(currentPeripheralIndex != position)
		{
			currentPeripheralIndex = position;

			final PeripheralInfo peripheralInfo = peripheralInfos.get(position);
			LOG.d("[Method:onPageSelected] position == " + position + "   BLE_ADDRESS == " + peripheralInfo.getMacAddress() +
					"   isConnected == " + peripheralInfo.isConnected());

			if (!peripheralInfo.isConnected())
			{
				handler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						HSBLEPeripheralManager.getInstance().scanTargetPeripheral(peripheralInfo.getMacAddress());
					}
				}, 2000);

				progressDialog.show();
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		PeripheralInfo peripheralInfo = (PeripheralInfo)getIntent().getSerializableExtra(IntentKeys.PERIPHERAL_INFO);
		connectedPeripheral = null != peripheralInfo ? peripheralInfo : PeripheralInfo.NULL_OBJECT;

		onPeripheralChanged();

		//读取蓝牙设备特征值
		HSBLEPeripheralManager.getInstance().readAllCharacteristics();
		progressDialog.show();
	}

	@OnClick({ R.id.common_function_btn, R.id.common_settings_btn , R.id.notification_switch, R.id.voice_switch,
	R.id.function_pinned_location_image, R.id.function_camera_shutter_image, R.id.function_find_item_image,
	R.id.function_voice_memos_image})
	public void onClick(View view)
	{
		if(view == getRightBarButton())
		{
			startActivity(new Intent(this, SettingActivity.class));
			return;
		}
		else if(view == getLeftBarButton())
		{
			startActivity(new Intent(this, LoginActivity.class));
			return;
		}

		switch (view.getId())
		{
			case R.id.common_function_btn:
				showFunctionBoard();
				break;

			case R.id.common_settings_btn:
				showSettingBoard();
				break;

			case R.id.function_pinned_location_image:
				pinnedLocationImg.setPressed(true);
				startPinnedLocationHistoryActivity();
				break;

			case R.id.function_camera_shutter_image:
				startCamera();
				break;

			case R.id.function_find_item_image:
				startFindItemActivity();
				break;

			case R.id.function_voice_memos_image:
				startVoiceMemosActivity();
				break;

			default:
				break; 
		}
	}

	@OnCheckedChanged({R.id.notification_switch, R.id.voice_switch, R.id.play_sound_switch})
	void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		switch (buttonView.getId())
		{
			case R.id.voice_switch:
				if(isChecked)
				{
					AppSettings.switchToRecordMode();
				}
				else
				{
					AppSettings.switchToLocateMode();
				}
				break;
			case R.id.notification_switch:
				AppSettings.setPushNotificationSetting(this, isChecked);
				break;

			case R.id.play_sound_switch:
				if(isChecked)
				{
					HSBLEPeripheralManager.getInstance().turnOnAlarmOfDisconnected();
				}
				else
				{
					HSBLEPeripheralManager.getInstance().turnOffAlarmOfDisconnected();
				}
				break;

			default:
				break;
		}
	}

	private void showFunctionBoard()
	{
		functionBord.setVisibility(View.VISIBLE);
		settingsBord.setVisibility(View.GONE);
		functionBtn.setBackgroundColor(getResources().getColor(R.color.color_val_4fc191));
		functionBtn.setTextColor(Color.WHITE);

		settingsBtn.setBackgroundResource(R.drawable.rectangle_4fc191_stroke_white_bg);
		settingsBtn.setTextColor(getResources().getColor(R.color.color_val_4fc191));
	}

	private void showSettingBoard()
	{
		functionBord.setVisibility(View.GONE);
		settingsBord.setVisibility(View.VISIBLE);

		settingsBtn.setBackgroundColor(getResources().getColor(R.color.color_val_4fc191));
		settingsBtn.setTextColor(Color.WHITE);

		functionBtn.setBackgroundResource(R.drawable.rectangle_4fc191_stroke_white_bg);
		functionBtn.setTextColor(getResources().getColor(R.color.color_val_4fc191));
	}

	private void startPinnedLocationHistoryActivity()
	{
		startActivity(new Intent(this, PinnedLocationHistoryActivity.class));
	}

	private void startFindItemActivity()
	{
		startActivity(new Intent(this, FindMyItemActivity.class));
	}

	private void startVoiceMemosActivity()
	{
		startActivity(new Intent(this, VoiceMemosActivity.class));
	}

	private void startCamera()
	{
		startActivity(Camera.class);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}

	@Override
	public void finish()
	{
		super.finish();
		TGApplicationProxy.getInstance().exit();
	}
}

package com.lepow.hiremote.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.lepow.hiremote.R;
import com.lepow.hiremote.app.BaseActivity;
import com.lepow.hiremote.authorise.ProfileActivity;
import com.lepow.hiremote.bluetooth.HSBLEPeripheralManager;
import com.lepow.hiremote.bluetooth.data.PeripheralInfo;
import com.lepow.hiremote.home.present.HomePresenter;
import com.lepow.hiremote.home.present.IHomeView;
import com.lepow.hiremote.lbs.FindMyItemActivity;
import com.lepow.hiremote.lbs.PinnedLocationHistoryActivity;
import com.lepow.hiremote.misc.ServerUrls;
import com.lepow.hiremote.record.VoiceMemosActivity;
import com.lepow.hiremote.setting.SettingActivity;
import com.mn.tiger.bluetooth.event.ConnectPeripheralEvent;
import com.mn.tiger.upgrade.TGUpgradeManager;
import com.mn.tiger.widget.TGNavigationBar;
import com.mn.tiger.widget.viewpager.DotIndicatorBannerPagerView;
import com.mn.tiger.widget.viewpager.DotIndicatorBannerPagerView.ViewPagerHolder;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.FindView;
import butterknife.OnClick;

public class HomeActivity extends BaseActivity implements IHomeView, View.OnClickListener
{
	@FindView(R.id.devices_viewpager)
	DotIndicatorBannerPagerView<PeripheralInfo> bannerPagerView;

	@FindView(R.id.common_function_btn)
	Button functionBtn;

	@FindView(R.id.common_function_bord)
	LinearLayout functionBord;

	@FindView(R.id.function_pinned_location_image)
	Button pinnedLocationImg;

	@FindView(R.id.function_camera_shutter_image)
	Button cameraShutterImg;

	@FindView(R.id.function_find_item_image)
	Button findItemImg;

	@FindView(R.id.function_voice_memos_image)
	Button voiceMemosImg;

	@FindView(R.id.common_settings_btn)
	Button settingsBtn;

	@FindView(R.id.common_settings_bord)
	LinearLayout settingsBord;

	@FindView(R.id.notification_switch)
	Switch notificationSwitch;

	@FindView(R.id.voice_switch)
	Switch voiceSwitch;

	@FindView(R.id.play_sound_switch)
	Switch playSoundSwitch;

	private HomePresenter presenter;

	private Button lastClickFunction;

	private HashMap<Integer, Integer> defaultFunctionBtnResMap;

	private HashMap<Integer, Integer> highlightFunctionBtnResMap;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().getDecorView().setBackgroundResource(R.drawable.home_page_bg);
		setBarTitleText(getString(R.string.app_name));

		ButterKnife.bind(this);

		initViews();

		presenter = new HomePresenter(this, this);

		presenter.initDevicesAndSettings();

		//检测更新
		TGUpgradeManager.upgrade(ServerUrls.CHECK_UPGRADE_URL);

		presenter.register2Bus();
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
		getRightBarButton().setImageResource(R.drawable.add_device);
		getRightBarButton().setOnClickListener(this);

		showLeftBarButton(true);
		getLeftBarButton().setImageResource(R.drawable.add_device);
		getLeftBarButton().setOnClickListener(this);

		bannerPagerView.setViewPagerHolder(new ViewPagerHolder<PeripheralInfo>()
		{
			@Override
			public View initViewOfPage(Activity activity, PagerAdapter adapter, int viewType)
			{
				return LayoutInflater.from(HomeActivity.this).inflate(R.layout.device_banner_item, null);
			}

			@Override
			public void fillData(View viewOfPage, PeripheralInfo itemData, int position, int viewType)
			{
				TextView deviceName = (TextView) viewOfPage.findViewById(R.id.peripheral_name);
				deviceName.setText(itemData.getPeripheralName());

				TextView syncTimeView = (TextView) viewOfPage.findViewById(R.id.peripheral_location);
				syncTimeView.setText(itemData.getSyncTime() + "");
			}
		});

		showFunctionBoard();
	}

	private void initFunctionBtnRes()
	{
		defaultFunctionBtnResMap.put(R.id.function_pinned_location_image, R.drawable.location_button_bg);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

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
			startActivity(new Intent(this, ProfileActivity.class));
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

			case R.id.notification_switch:
				presenter.turnOnOrOffNotificationSetting();
				break;

			case R.id.voice_switch:
				presenter.turnOnOrOffVoiceSetting();
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

	private void showFunctionBoard()
	{
		functionBord.setVisibility(View.VISIBLE);
		settingsBord.setVisibility(View.GONE);
		functionBtn.setBackgroundColor(Color.GREEN);
		functionBtn.setTextColor(Color.WHITE);

		settingsBtn.setBackgroundColor(Color.WHITE);
		settingsBtn.setTextColor(Color.GREEN);
	}

	private void showSettingBoard()
	{
		functionBord.setVisibility(View.GONE);
		settingsBord.setVisibility(View.VISIBLE);

		settingsBtn.setBackgroundColor(Color.GREEN);
		settingsBtn.setTextColor(Color.WHITE);

		functionBtn.setBackgroundColor(Color.WHITE);
		functionBtn.setTextColor(Color.GREEN);
	}

	@Override
	public void initDeviceBanner(List<PeripheralInfo> peripheralInfos)
	{
		bannerPagerView.setData(peripheralInfos);
	}

	@Override
	public void turnOnNotificationSetting()
	{
		notificationSwitch.setChecked(true);
	}

	@Override
	public void turnOffNotificationSetting()
	{
		notificationSwitch.setChecked(false);
	}

	@Override
	public void turnOnVoiceSetting()
	{
		voiceSwitch.setChecked(true);
	}

	@Override
	public void turnOffVoiceSetting()
	{
		voiceSwitch.setChecked(false);
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
		//TODO
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		presenter.unregisterFromBus();
	}

	@Subscribe
	public void onConnectDevice(final ConnectPeripheralEvent event)
	{
		switch (event.getState())
		{
			case Connected:
				break;

			case Disconnect:
				break;

			case BluetoothOff:
				HSBLEPeripheralManager.getInstance().showBluetoothOffDialog(this);
				break;

			default:
				break;
		}
	}

}

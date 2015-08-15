package com.lepow.hiremote.connect;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lepow.hiremote.R;
import com.lepow.hiremote.app.BaseActivity;
import com.lepow.hiremote.app.HSApplication;
import com.lepow.hiremote.connect.data.DeviceInfo;
import com.lepow.hiremote.home.HomeActivity;
import com.lepow.hiremote.misc.IntentKeys;
import com.lepow.hiremote.widget.HSAlertDialog;
import com.mn.tiger.bluetooth.event.Connect2DeviceEvent;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.FindView;
import butterknife.OnClick;

/**
 * 设备扫描界面
 */
public class ScanDeviceActivity extends BaseActivity
{
	@FindView(R.id.scan_device_progress)
	ProgressBar scanProgressBar;

	@FindView(R.id.scan_device_cancel_btn)
	Button cancelBtn;

	@FindView(R.id.scan_device_retry_btn)
	Button retryBtn;

	@FindView(R.id.connect_success_layout)
	RelativeLayout connectSuccessLayout;

	private static Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_device_activity);
		ButterKnife.bind(this);

		//开始扫描设备
		scanDevice();

		HSApplication.getBus().register(this);
	}

	private void scanDevice()
	{
		scanProgressBar.setVisibility(View.VISIBLE);
		cancelBtn.setVisibility(View.VISIBLE);
		retryBtn.setVisibility(View.VISIBLE);
	}

	@OnClick({R.id.scan_device_cancel_btn, R.id.scan_device_retry_btn})
	public void onClick(View view)
	{
		switch (view.getId())
		{
			case R.id.scan_device_cancel_btn:
				gotoHomeActivity(null);
				break;

			case R.id.scan_device_retry_btn:
				scanDevice();
				break;

			default:
				break;
		}
	}

	@Subscribe
	public void onConnectDevice(final Connect2DeviceEvent event)
	{
		switch (event.getState())
		{
			case Success:
				connectSuccessLayout.setVisibility(View.VISIBLE);
				handler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						gotoHomeActivity(event.getDeviceInfo());
						finish();
					}
				}, 2000);
				break;

			case Disconnect:
				break;

			default:
				break;
		}
	}

	/**
	 * 显示蓝牙未开启对话框
	 */
	private void showBluetoothOffDialog()
	{
		HSAlertDialog dialog = new HSAlertDialog(this);
		dialog.setBodyText(getString(R.string.bluetooth_off_tips));
		dialog.setLeftButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		dialog.setRightButton(getString(R.string.open_bluetooth_now), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				startBluetoothSettingActivity();
			}
		});
	}

	/**
	 * 显示不支持蓝牙4.0的对话框
	 */
	private void showNonSupportBLEDialog()
	{
		HSAlertDialog dialog = new HSAlertDialog(this);
		dialog.setBodyText(getString(R.string.nonsupport_bluetooth_tips));
		dialog.setLeftButton(getString(R.string.exit_now), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				HSApplication.getInstance().exit();
			}
		});
	}

	/**
	 * 进入主界面
	 * @param deviceInfo
	 */
	private void gotoHomeActivity(DeviceInfo deviceInfo)
	{
		Intent intent = new Intent(this, HomeActivity.class);
		intent.putExtra(IntentKeys.DEVICE_INFO, deviceInfo);
		startActivity(intent);
	}

	private void startBluetoothSettingActivity()
	{

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HSApplication.getBus().unregister(this);
	}
}

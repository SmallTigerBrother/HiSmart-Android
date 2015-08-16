package com.lepow.hiremote.bluetooth;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.lepow.hiremote.R;
import com.lepow.hiremote.app.BaseActivity;
import com.lepow.hiremote.bluetooth.data.PeripheralInfo;
import com.lepow.hiremote.home.HomeActivity;
import com.lepow.hiremote.misc.IntentKeys;
import com.lepow.hiremote.widget.HSAlertDialog;

import butterknife.ButterKnife;
import butterknife.FindView;
import butterknife.OnClick;

/**
 * Created by peng on 15/8/16.
 */
public class RenamePeripheralActivity extends BaseActivity
{
    @FindView(R.id.peripheral_rename_btn)
    Button renamePeripheralBtn;

    private PeripheralInfo peripheralInfo;

    private String renameHint;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rename_peripheral_activity);
        ButterKnife.bind(this);

        peripheralInfo = (PeripheralInfo)getIntent().getSerializableExtra(IntentKeys.PERIPHERAL_INFO);

        renameHint = getString(R.string.rename_peripheral_btn_text, peripheralInfo.getPeripheralName());
        renamePeripheralBtn.setText(renameHint);
    }

    @OnClick({R.id.peripheral_rename_btn, R.id.peripheral_rename_next_btn})
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.peripheral_rename_btn:
                showRenameDialog();
                break;

            case R.id.peripheral_rename_next_btn:
                Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra(IntentKeys.PERIPHERAL_INFO, peripheralInfo);
                startActivity(intent);
                finish();
                break;

            default:
                break;
        }
    }

    private void showRenameDialog()
    {
        HSAlertDialog dialog = new HSAlertDialog(this);
        dialog.setTitleVisibility(View.GONE);

        final EditText renameView = new EditText(this);
        renameView.setHint(renameHint);

        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.margin_val_20px);
        layoutParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.margin_val_20px);
        layoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen.margin_val_20px);
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margin_val_20px);
        dialog.setBodyContentView(renameView, layoutParams);

        dialog.setLeftButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        dialog.setRightButton(getString(R.string.confirm), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String newName = renameView.getText().toString().trim();
                if(!TextUtils.isEmpty(newName))
                {
                    renamePeripheralBtn.setText(newName);
                }
                dialog.dismiss();
            }
        });
    }
}
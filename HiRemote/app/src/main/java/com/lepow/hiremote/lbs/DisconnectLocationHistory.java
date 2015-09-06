package com.lepow.hiremote.lbs;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.lepow.hiremote.R;
import com.lepow.hiremote.app.BaseActivity;
import com.lepow.hiremote.lbs.adapter.LocationViewHolder;
import com.lepow.hiremote.lbs.data.LocationDataManager;
import com.lepow.hiremote.lbs.data.LocationInfo;
import com.lepow.hiremote.misc.ActivityResultCode;
import com.lepow.hiremote.misc.IntentKeys;
import com.lepow.hiremote.widget.HSAlertDialog;
import com.mn.tiger.widget.TGNavigationBar;
import com.mn.tiger.widget.adpter.TGListAdapter;

import butterknife.ButterKnife;
import butterknife.FindView;

/**
 * 断开连接的地址历史界面
 */
public class DisconnectLocationHistory extends BaseActivity
{
    @FindView(R.id.disconnect_location_history)
    SwipeMenuListView listView;

    private TGListAdapter<LocationInfo> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disconnect_location_history);
        setBarTitleText(getString(R.string.disconnection_history));
        ButterKnife.bind(this);

        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);
        listView.setMenuCreator(new SwipeMenuCreator()
        {
            @Override
            public void create(SwipeMenu swipeMenu)
            {
                SwipeMenuItem editItem = new SwipeMenuItem(DisconnectLocationHistory.this);
                editItem.setIcon(R.drawable.location_edit);
                editItem.setBackground(new ColorDrawable(getResources().getColor(R.color.color_val_d9d9d9)));
                editItem.setWidth(getResources().getDimensionPixelSize(R.dimen.margin_val_120px));
                swipeMenu.addMenuItem(editItem);

                SwipeMenuItem deleteItem = new SwipeMenuItem(DisconnectLocationHistory.this);
                deleteItem.setIcon(R.drawable.location_trash);
                deleteItem.setBackground(new ColorDrawable(getResources().getColor(R.color.color_val_d9d9d9)));
                deleteItem.setWidth(getResources().getDimensionPixelSize(R.dimen.margin_val_120px));
                swipeMenu.addMenuItem(deleteItem);
            }
        });

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu swipeMenu, int index)
            {
                switch (index)
                {
                    case 0:
                        openEditLocationDialog((LocationInfo) listAdapter.getItem(position));
                        return true;

                    case 1:
                        openDeleteConfirmDialog((LocationInfo) listAdapter.getItem(position));
                        return true;

                    default:
                        return false;
                }
            }
        });

        listAdapter = new TGListAdapter<LocationInfo>(this, LocationDataManager.getInstance().findAllDisconnectedLocationOderByTime(this),
                R.layout.location_list_item, LocationViewHolder.class);
        listAdapter.setStrictlyReuse(true);
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void initNavigationResource(TGNavigationBar navigationBar)
    {
        super.initNavigationResource(navigationBar);
        navigationBar.setBackgroundResource(R.drawable.navi_bar_bg);
    }

    private void openDeleteConfirmDialog(final LocationInfo locationInfo)
    {
        HSAlertDialog dialog = new HSAlertDialog(this);
        dialog.setTitleVisibility(View.GONE);
        dialog.setBodyText(getString(R.string.make_sure_you_delete_location));
        //设置取消按钮
        dialog.setLeftButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        //设置确定按钮
        dialog.setRightButton(getString(R.string.confirm), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                removeLocation(locationInfo);
                //更新数据库
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * 删除一条定位记录
     * @param itemData
     */
    private void removeLocation(LocationInfo itemData)
    {
        switch (itemData.getDataType())
        {
            case LocationInfo.DATA_TYPE_PINNED_LOCATION:
                LocationDataManager.getInstance().removePinnedLocation(this, itemData);
                break;

            case LocationInfo.DATA_TYPE_DISCONNECT_LOCATION:
                LocationDataManager.getInstance().removeDisconnectedLocation(this, itemData);
                break;

            default:
                break;
        }

        listAdapter.removeItem(itemData);
    }

    /**
     *启动地图界面
     * @param dataType
     */
    private void startMapActivity(int dataType, LocationInfo locationInfo)
    {
        Intent intent = new Intent();
        switch (dataType)
        {
            case LocationInfo.DATA_TYPE_PINNED_LOCATION:
                //启动地图界面
                intent.setClass(this, PinnedLocationMapActivity.class);
                this.startActivity(intent);
                break;

            case LocationInfo.DATA_TYPE_DISCONNECT_LOCATION:
                //返回到上一界面
                intent.putExtra(IntentKeys.LOCATION_INFO, locationInfo);
                this.setResult(ActivityResultCode.DISCONNECT_LOCATION_HISTORY, intent);
                this.finish();

                break;
            default:
                break;
        }
    }

    /**
     * 打开编辑对话框
     */
    private void openEditLocationDialog(final LocationInfo locationInfo)
    {
        HSAlertDialog dialog = new HSAlertDialog(this);
        dialog.setTitleText(getString(R.string.edit_location_title));
        //设置输入框
        final EditText editText = new EditText(this);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setTextColor(getResources().getColor(R.color.text_color_normal));
        String address = TextUtils.isEmpty(locationInfo.getRemark()) ? locationInfo.getAddress() : locationInfo.getRemark();
        editText.setText(address);
        editText.setSelection(address.length());
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText.setPadding(getResources().getDimensionPixelSize(R.dimen.margin_val_20px),
                getResources().getDimensionPixelSize(R.dimen.margin_val_20px),
                getResources().getDimensionPixelSize(R.dimen.margin_val_20px),
                getResources().getDimensionPixelSize(R.dimen.margin_val_30px));
        dialog.setBodyContentView(editText, layoutParams);
        //设置取消按钮
        dialog.setLeftButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(editText.getWindowToken(), 0);
                dialog.dismiss();
            }
        });

        //设置确定按钮
        dialog.setRightButton(getString(R.string.confirm), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //更新数据库
                locationInfo.setRemark(editText.getText().toString().trim());
                switch (locationInfo.getDataType())
                {
                    case LocationInfo.DATA_TYPE_PINNED_LOCATION:
                        LocationDataManager.getInstance().savePinnedLocation(DisconnectLocationHistory.this, locationInfo);
                        break;

                    case LocationInfo.DATA_TYPE_DISCONNECT_LOCATION:
                        LocationDataManager.getInstance().saveDisconnectedLocation(DisconnectLocationHistory.this, locationInfo);
                        break;
                    default:
                        break;
                }
                //更新列表
                listAdapter.notifyDataSetChanged();
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(editText.getWindowToken(), 0);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}

package com.android.libcore_ui.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.libcore.activity.RootActivity;
import com.android.libcore.log.L;
import com.android.libcore.utils.CommonUtils;
import com.android.libcore_ui.R;
import com.android.libcore_ui.widget.BottomBarGroupLinearLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Description: 继承自{@link RootActivity}的基础activity，在这里进行页面界面
 * 的统一,在这里将样式定位一个仿actionbar的样式，但是为了方便使用和扩展，所以将会使用一个
 * {@link RelativeLayout}做一个actionbar
 *
 * <ol>
 * <li>{@linkplain #receiver}用来在组件之间进行广播的接收</li>
 * <li>{@linkplain #initView()}用来初始化该activity的view，第一步调用{@link #setContentViewSrc(Object)}
 * 进行设置布局，参数为layout的id或者view，在里面进行{@link #findViewById(int)}等等操作</li>
 * <li>{@linkplain #initData()}用来初始化该activity的data</li>
 * </ol>
 *
 * 自定义底部弹出框:
 * <ul>
 * <li>{@link #addItemToBottomPopWindow(int, int, String)}方法用来在底部弹出的框内加上按钮选项，有组id，元素id,
 * 和元素名称来标识,显示的上下顺序将会按照添加时候的顺序显示，如果需要在中间插入一个组元素，则初始化添加的时候调用
 * {@link #addItemToBottomPopWindow(int, int, String)}函数的时候，groupId传一个新值，itemId传递一个小于0
 * 的数值即可，代表先占一个空位，方便以后来对该groupId位置的元素进行操作</li>
 * <li>{@link #removeItemFromBottomPopWindow(int, int)}方法用来删除在底部添加的按钮选项</li>
 * <li>{@link #showBottomPopWindow()}方法用来显示底部popwindow，调用之前请先调用
 * {@link #addItemToBottomPopWindow(int, int, String)}方法</li>
 * <li>{@link #onItemClickCallback(int, int)}方法由子类继承用来处理底部弹出框的点击回调</li>
 * </ul>
 *
 * <strong>{@linkplain #initView()}和{@linkplain #initData()}需要子类实现</strong>
 *
 * @author zzp(zhao_zepeng@hotmail.com)
 * @since 2015-07-08
 */
public abstract class BaseActivity extends RootActivity{

    /** 头部bar，如果某些activity需要改变bar样式，修改该view的子view即可 */
    protected ViewGroup ll_top_content;
    /** 返回按钮 */
    protected RelativeLayout rl_back;
    /** 该页面标题 */
    protected TextView tv_title;
    /** 右侧添加按钮区域 */
    protected RelativeLayout rl_top_extra_content;
    /** 内容区域 */
    protected FrameLayout content;
    /** 全屏的半透明显示 */
    protected View ll_full_screen;
    /** 底部popWindow */
    protected ScrollView sv_bottom_content;
    protected LinearLayout ll_bottom_content;
    /** 底部弹出框数据集合 */
    protected LinkedHashMap<Integer, ArrayList<ItemHolder>> bottomItems;

    protected LayoutInflater inflater;
    protected ObjectAnimator popAnimation;
    protected ObjectAnimator reverseAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_base_layout);
        ll_top_content = (ViewGroup) findViewById(R.id.ll_top_content);
        rl_back = (RelativeLayout) findViewById(R.id.rl_back);
        tv_title = (TextView) findViewById(R.id.tv_title);
        rl_top_extra_content = (RelativeLayout) findViewById(R.id.rl_top_extra_content);
        content = (FrameLayout) findViewById(R.id.content);
        ll_full_screen = findViewById(R.id.ll_full_screen);
        ll_full_screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doReverseAnimation();
            }
        });
        sv_bottom_content = (ScrollView) findViewById(R.id.sv_bottom_content);
        ll_bottom_content = (LinearLayout) findViewById(R.id.ll_bottom_content);

        /** 通过 android::label 设置的标题 */
        if (!TextUtils.isEmpty(getTitle()))
            tv_title.setText(getTitle());
        rl_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        bottomItems = new LinkedHashMap<>();
        inflater = LayoutInflater.from(this);

        initView();
        initData();
    }

    /** <Strong><font color=red>第一步</font></Strong>调用{@link #setContentViewSrc(Object)}进行布局的设置 */
    protected abstract void initView();
    protected abstract void initData();

    /**
     * 设置标题
     */
    protected void setTitle(String title){
        tv_title.setText(title);
    }

    /**
     * 用来设置该页面需要显示的内容，不包括topbar
     * @param object
     */
    protected void setContentViewSrc(Object object){
        if (object instanceof Integer){
            LayoutInflater inflater = LayoutInflater.from(this);
            View v = inflater.inflate((Integer)object, null);
            content.addView(v);
        }else if(object instanceof View){
            content.addView((View)object);
        }else{
            L.e("参数只能为id或者view");
        }
    }

    /**
     * 底部item的数据集合
     */
    private class ItemHolder{
        private int itemId;
        private String name;
    }

    /**
     * 通过添加item到底部bar来创建一系列的选项
     * @param groupId 该item的组id，不同的组id在不同的区域内,请使用大于0的数字来表示
     * @param itemId 该item的item id，用来标示该item，组内的两个item不能有相同的item id,要不然回调无法识别
     * @param name 用来显示该item的名字
     */
    protected void addItemToBottomPopWindow(int groupId, int itemId, String name){
        ArrayList<ItemHolder> temp = null;
        if (bottomItems.containsKey(groupId)) {
            if (itemId < 0){
                throw new IllegalArgumentException("groupId can be found,so itemId must bigger than 0 or equal 0");
            }
            temp = bottomItems.get(groupId);
            ItemHolder holder = new ItemHolder();
            holder.itemId = itemId;
            holder.name = name;
            temp.add(holder);
        }
        else {
            temp = new ArrayList<>();
            if (itemId >= 0) {
                ItemHolder holder = new ItemHolder();
                holder.itemId = itemId;
                holder.name = name;
                temp.add(holder);
            }
            bottomItems.put(groupId, temp);
        }
        buildBottomPopWindow();
    }

    /**
     * 将item从底部bar中删除
     * @param groupId
     * @param itemId
     */
    protected void removeItemFromBottomPopWindow(int groupId, int itemId){
        if (bottomItems.containsKey(groupId)){
            ArrayList<ItemHolder> temp = bottomItems.get(groupId);
            for (ItemHolder holder : temp){
                if (holder.itemId == itemId){
                    temp.remove(holder);
                    buildBottomPopWindow();
                    return;
                }
            }
            throw new IllegalArgumentException("can't find this itemId in this groupId");
        }else{
            throw new IllegalArgumentException("can't find this groupId");
        }
    }

    /**
     * 通过{@link #bottomItems}建立底部弹出框
     */
    private void buildBottomPopWindow(){
        if (bottomItems.size() <= 0)
            return;
        //现将底部弹出框的所有选项去除
        ll_bottom_content.removeAllViews();
        final Iterator iterator = bottomItems.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Integer, ArrayList<ItemHolder>> entry = (Map.Entry<Integer, ArrayList<ItemHolder>>) iterator.next();
            Integer groupId = entry.getKey();
            ArrayList<ItemHolder> holder = entry.getValue();
            //如果该groupId的items不为0，代表需要将该group显示出来
            if (holder.size() >= 0){
                BottomBarGroupLinearLayout group = (BottomBarGroupLinearLayout) inflater.inflate(R.layout.bottom_group_layout, null);
                group.setGroupId(groupId);
                group.setCallback(new BottomBarGroupLinearLayout.GroupItemClickCallback() {
                    @Override
                    public void callback(int groupId, int itemId) {
                        onItemClickCallback(groupId, itemId);
                    }
                });
                for (ItemHolder temp : holder){
                    group.addItemToGroup(temp.itemId, temp.name);
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int margin = CommonUtils.dp2px(10);
                params.setMargins(margin, 0, margin, margin);
                ll_bottom_content.addView(group, params);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (sv_bottom_content.getVisibility() == View.VISIBLE){
            doReverseAnimation();
        }else {
            super.onBackPressed();
        }
    }

    /**
     * 点击底部弹出框的回调
     */
    protected void onItemClickCallback(int groupId, int itemId){
        doReverseAnimation();
    }

    private void doReverseAnimation(){
        ll_full_screen.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT < 11) {
            sv_bottom_content.setVisibility(View.GONE);
        }else{
            if (reverseAnimation == null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv_bottom_content.getLayoutParams();
                reverseAnimation = ObjectAnimator.ofInt(sv_bottom_content, "bottomMargin", 0, -params.bottomMargin);
                reverseAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv_bottom_content.getLayoutParams();
                        params.bottomMargin = value;
                        sv_bottom_content.setLayoutParams(params);
                        ((View) (sv_bottom_content.getParent())).invalidate();
                    }
                });
                reverseAnimation.setDuration(500);
            }
            reverseAnimation.start();
        }
    }

    /**
     * 用来显示该popwindow，保证在调用该方法之前已经调用{@link #addItemToBottomPopWindow(int, int, String)}方法
     */
    protected void showBottomPopWindow(){
        ll_full_screen.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 11) {
            if (popAnimation == null) {
                int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                sv_bottom_content.measure(width, width);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv_bottom_content.getLayoutParams();
                popAnimation = ObjectAnimator.ofInt(sv_bottom_content, "bottomMargin", -params.bottomMargin, 0);
                popAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv_bottom_content.getLayoutParams();
                        params.bottomMargin = value;
                        sv_bottom_content.setLayoutParams(params);
                        ((View) (sv_bottom_content.getParent())).invalidate();
                    }
                });
                popAnimation.setDuration(500);
            }
            popAnimation.start();
        }else{
            sv_bottom_content.setVisibility(View.VISIBLE);
        }
    }
}

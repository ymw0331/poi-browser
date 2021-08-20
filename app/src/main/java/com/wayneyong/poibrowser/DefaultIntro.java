package com.wayneyong.poibrowser;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;

public class DefaultIntro extends AppIntro {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide1_title)
                , getResources().getString(R.string.slide1_desc)
                , R.drawable.slide1
                , getResources().getColor(R.color.white)
                , getResources().getColor(R.color.primaryColor)
                , getResources().getColor(R.color.primaryDarkColor)));

        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide2_title)
                , getResources().getString(R.string.slide2_desc)
                , R.drawable.slide2
                , getResources().getColor(R.color.white)
                , getResources().getColor(R.color.primaryColor)
                , getResources().getColor(R.color.primaryDarkColor)));

        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide3_title)
                , getResources().getString(R.string.slide3_desc)
                , R.drawable.slide3
                , getResources().getColor(R.color.white)
                , getResources().getColor(R.color.primaryColor)
                , getResources().getColor(R.color.primaryDarkColor)));

        setBarColor(getResources().getColor(R.color.accentColor));
//        showSkipButton(false);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }


}

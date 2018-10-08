package me.tsukanov.counter.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import me.tsukanov.counter.CounterApplication;
import me.tsukanov.counter.R;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_TITLE = "title";
    private static final String STATE_IS_NAV_OPEN = "is_nav_open";
    public static final String STATE_ACTIVE_COUNTER = "activeKey";
    private static final String PREF_KEEP_SCREEN_ON = "keepScreenOn";
    private static final String PREF_THEME = "theme";
    private static final String THEME_DARK = "dark";
    private static final String THEME_LIGHT = "light";
    public CountersListFragment countersListFragment;
    public CounterFragment currentCounter;
    private CounterApplication app;
    private ActionBar actionBar;
    private DrawerLayout navigationLayout;
    private ActionBarDrawerToggle navigationToggle;
    private CharSequence title;
    private SharedPreferences sharedPref;
    private FrameLayout menuFrame;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getString(PREF_THEME, THEME_LIGHT).equals(THEME_DARK)) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView tdate = (TextView) findViewById(R.id.end_time);
                                long date = System.currentTimeMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy\nhh-mm-ss a");
                                String dateString = sdf.format(date);
                                tdate.setText("End Time " + dateString);
                            }
                        });

                    }
                }catch(InterruptedException e){
                }
            }
        };
        t.start();

        app = (CounterApplication) getApplication();

        final CharSequence drawerTitle = title = getTitle();
        navigationLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        menuFrame = (FrameLayout) findViewById(R.id.menu_frame);

        actionBar = getSupportActionBar();

        // Enable ActionBar home button to behave as action to toggle navigation drawer
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        navigationToggle = new ActionBarDrawerToggle(
                this,
                navigationLayout,
                null,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                title = currentCounter.getName();
                actionBar.setTitle(title);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                actionBar.setTitle(drawerTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        navigationLayout.setDrawerListener(navigationToggle);

        if (savedInstanceState != null) {
            title = savedInstanceState.getCharSequence(STATE_TITLE);
            if (savedInstanceState.getBoolean(STATE_IS_NAV_OPEN)) {
                actionBar.setTitle(drawerTitle);
            } else {
                actionBar.setTitle(title);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
        countersListFragment = new CountersListFragment();
        transaction.replace(R.id.menu_frame, countersListFragment);
        transaction.commit();

        String previousCounter = sharedPref.getString(STATE_ACTIVE_COUNTER, getString(R.string.default_counter_name));
        switchCounterFragment(new CounterFragment(previousCounter));

        if (sharedPref.getBoolean(PREF_KEEP_SCREEN_ON, false)) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.saveCounters();
        SharedPreferences.Editor settingsEditor = sharedPref.edit();
        settingsEditor.putString(STATE_ACTIVE_COUNTER, currentCounter.getCounterName());
        settingsEditor.commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event) || currentCounter.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        navigationToggle.syncState();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putCharSequence(STATE_TITLE, title);
        savedInstanceState.putBoolean(STATE_IS_NAV_OPEN, navigationLayout.isDrawerOpen(menuFrame));
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        navigationToggle.onConfigurationChanged(newConfig);
    }

    public void searchCounter(){

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (navigationLayout.isDrawerOpen(menuFrame)) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                return true;
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean isNavigationOpen() {
        return navigationLayout.isDrawerOpen(menuFrame);
    }

    public void switchCounterFragment(final CounterFragment fragment) {
        currentCounter = fragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, currentCounter).commit();
        closeDrawer();
    }

    public void closeDrawer() {
        navigationLayout.closeDrawer(menuFrame);
    }

    public void openDrawer() {
        navigationLayout.openDrawer(menuFrame);
    }

}

/*
 * Copyright (C) 2020 YAAP
 * Copyright (C) 2023-2024 cyberknight777
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.fastcharge.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.fastcharge.R;
import com.android.fastcharge.utils.FileUtils;

/**
 * Fragment that handles the Fast Charging feature settings
 * This allows users to enable/disable fast charging functionality
 */
public class FastChargeFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    // UI component for toggling fast charge
    private SwitchPreferenceCompat mFastChargePreference;
    
    // Configuration helper that stores paths and constants
    private FastChargeConfig mConfig;
    
    // Flag to prevent infinite broadcast loops
    private boolean mInternalFastChargeStart = false;

    /**
     * BroadcastReceiver that listens for changes to the fast charge service
     * This keeps the UI in sync with the actual system state
     */
    private final BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Check if this is a fast charge service change notification
            if (action.equals(mConfig.ACTION_FAST_CHARGE_SERVICE_CHANGED)) {
                // Ignore if we're the ones who triggered this broadcast
                if (mInternalFastChargeStart) {
                    mInternalFastChargeStart = false;
                    return;
                }

                if (mFastChargePreference == null) return;

                // Get the current state from the broadcast
                final boolean fastchargeStarted = intent.getBooleanExtra(
                        mConfig.EXTRA_FAST_CHARGE_STATE, false);

                // Update the switch to match actual system state
                mFastChargePreference.setChecked(fastchargeStarted);
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load preferences from XML resource
        setPreferencesFromResource(R.xml.fastcharge_settings, rootKey);
        
        // Initialize configuration helper
        mConfig = FastChargeConfig.getInstance(getContext());
        
        // Find and set up the fast charge toggle switch
        mFastChargePreference = (SwitchPreferenceCompat) findPreference(mConfig.FASTCHARGE_KEY);
        
        // Check if fast charging is supported on this device
        if (FileUtils.fileExists(mConfig.getFastChargePath())) {
            // Fast charging is supported, enable the toggle
            mFastChargePreference.setEnabled(true);
            mFastChargePreference.setOnPreferenceChangeListener(this);
        } else {
            // Fast charging is not supported, disable the toggle and show message
            mFastChargePreference.setSummary(R.string.fast_charging_summary_not_supported);
            mFastChargePreference.setEnabled(false);
        }

        // Set initial state based on current system configuration
        mFastChargePreference.setChecked(mConfig.isCurrentlyEnabled(mConfig.getFastChargePath()));

        // Register broadcast receiver to listen for fast charge state changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(mConfig.ACTION_FAST_CHARGE_SERVICE_CHANGED);
        getContext().registerReceiver(mServiceStateReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh state when returning to this screen
        mFastChargePreference.setChecked(mConfig.isCurrentlyEnabled(mConfig.getFastChargePath()));
    }

    /**
     * Handles changes to preferences, specifically the fast charge toggle
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mConfig.FASTCHARGE_KEY.equals(preference.getKey())) {
            // Set flag to prevent handling our own broadcast
            mInternalFastChargeStart = true;
            Context mContext = getContext();

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            // Write the new value to the system file
            // "1" enables fast charging, "0" disables it
            FileUtils.writeLine(mConfig.getFastChargePath(), (Boolean) newValue ? "1" : "0");

            // Double-check that the change was applied successfully
            boolean enabled = mConfig.isCurrentlyEnabled(mConfig.getFastChargePath());

            // Save the current state in preferences
            sharedPrefs.edit().putBoolean(mConfig.FASTCHARGE_KEY, enabled).apply();

            // Broadcast the change so other components can react
            Intent intent = new Intent(mConfig.ACTION_FAST_CHARGE_SERVICE_CHANGED);
            intent.putExtra(mConfig.EXTRA_FAST_CHARGE_STATE, enabled);
            intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up by unregistering the receiver when fragment is destroyed
        getContext().unregisterReceiver(mServiceStateReceiver);
    }
                    }

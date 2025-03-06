/*
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
import android.os.UserHandle;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;

import com.android.fastcharge.R;
import com.android.fastcharge.utils.FileUtils;

/**
 * Quick Settings tile service for Fast Charging
 * Allows toggling fast charging directly from quick settings panel
 */
public class FastChargeTileService extends TileService {

    // Configuration helper for fast charge settings
    private FastChargeConfig mConfig;

    // Intent to track associated service
    private Intent mFastChargeIntent;

    // Flag to prevent handling our own broadcasts
    private boolean mInternalStart;

    /**
     * BroadcastReceiver that listens for fast charge state changes
     */
    private final BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Ignore if we triggered this broadcast ourselves
            if (mInternalStart) {
                mInternalStart = false;
                return;
            }
            // Update tile UI to reflect new state
            updateUI();
        }
    };

    /**
     * Updates the tile appearance based on current fast charge state
     */
    private void updateUI() {
        final Tile tile = getQsTile();
        // Check if fast charge is currently enabled
        boolean enabled = mConfig.isCurrentlyEnabled(mConfig.getFastChargePath());

        // If disabled, stop any related service
        if (!enabled) tryStopService();

        // Update tile appearance (active = enabled, inactive = disabled)
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    /**
     * Called when the tile becomes visible
     */
    @Override
    public void onStartListening() {
        super.onStartListening();
        // Initialize configuration
        mConfig = FastChargeConfig.getInstance(this);

        // Update tile state
        updateUI();

        // Register for fast charge state change broadcasts
        IntentFilter filter = new IntentFilter(mConfig.ACTION_FAST_CHARGE_SERVICE_CHANGED);
        registerReceiver(mServiceStateReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    /**
     * Called when the tile is no longer visible
     */
    @Override
    public void onStopListening() {
        super.onStopListening();
        // Clean up broadcast receiver
        unregisterReceiver(mServiceStateReceiver);
    }

    /**
     * Handles tile clicks to toggle fast charge
     */
    @Override
    public void onClick() {
        super.onClick();
        // Set flag to prevent handling our own broadcast
        mInternalStart = true;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Toggle the current state
        boolean enabled = !mConfig.isCurrentlyEnabled(mConfig.getFastChargePath());
        // Write new state to system file - "1" enables, "0" disables
        FileUtils.writeLine(mConfig.getFastChargePath(), enabled ? "1" : "0");

        // Save state in preferences
        sharedPrefs.edit().putBoolean(mConfig.FASTCHARGE_KEY, enabled).apply();

        // Broadcast change so other components can update
        Intent intent = new Intent(mConfig.ACTION_FAST_CHARGE_SERVICE_CHANGED);
        intent.putExtra(mConfig.EXTRA_FAST_CHARGE_STATE, enabled);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        this.sendBroadcastAsUser(intent, UserHandle.CURRENT);

        // Update this tile's appearance
        updateUI();
    }

    /**
     * Stops any associated service if running
     */
    private void tryStopService() {
        if (mFastChargeIntent == null) return;
        this.stopService(mFastChargeIntent);
        mFastChargeIntent = null;
    }
}

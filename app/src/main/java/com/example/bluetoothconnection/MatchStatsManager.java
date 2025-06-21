package com.example.bluetoothconnection;

import android.content.Context;
import android.content.SharedPreferences;

public class MatchStatsManager {
    private static final String PREF_NAME = "match_stats";
    private static final String KEY_PLAYED = "games_played";
    private static final String KEY_WON = "games_won";
    private static final String KEY_LOST = "games_lost";
    private static final String KEY_STREAK = "win_streak";

    public static void recordGame(Context context, boolean isWin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int played = prefs.getInt(KEY_PLAYED, 0) + 1;
        editor.putInt(KEY_PLAYED, played);

        if (isWin) {
            int won = prefs.getInt(KEY_WON, 0) + 1;
            int streak = prefs.getInt(KEY_STREAK, 0) + 1;
            editor.putInt(KEY_WON, won);
            editor.putInt(KEY_STREAK, streak);
        } else {
            int lost = prefs.getInt(KEY_LOST, 0) + 1;
            editor.putInt(KEY_LOST, lost);
            editor.putInt(KEY_STREAK, 0); // reset streak
        }

        editor.apply();
    }

    public static int getGamesPlayed(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_PLAYED, 0);
    }

    public static int getGamesWon(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_WON, 0);
    }

    public static int getGamesLost(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_LOST, 0);
    }

    public static int getWinStreak(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_STREAK, 0);
    }

    public static void resetStats(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
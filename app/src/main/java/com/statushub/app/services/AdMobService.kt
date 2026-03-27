package com.statushub.app.services

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.statushub.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service class for managing AdMob ads
 */
@Singleton
class AdMobService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Test IDs from BuildConfig
    private val interstitialAdId = BuildConfig.ADMOB_INTERSTITIAL_ID
    private val rewardedAdId = BuildConfig.ADMOB_REWARDED_ID

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    /**
     * Preload interstitial ad
     */
    fun preloadInterstitialAd() {
        if (interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        
        InterstitialAd.load(
            context,
            interstitialAdId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            preloadInterstitialAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Ad shown
                        }
                    }
                }
            }
        )
    }

    /**
     * Show interstitial ad if available
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdNotAvailable: () -> Unit = {}
    ) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            onAdShown()
        } else {
            onAdNotAvailable()
            preloadInterstitialAd()
        }
    }

    /**
     * Preload rewarded ad
     */
    fun preloadRewardedAd() {
        if (rewardedAd != null || isRewardedLoading) return

        isRewardedLoading = true
        
        RewardedAd.load(
            context,
            rewardedAdId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            preloadRewardedAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Ad shown
                        }
                    }
                }
            }
        )
    }

    /**
     * Show rewarded ad if available
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdNotAvailable: () -> Unit = {}
    ) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                // Reward earned
                onRewardEarned()
            }
        } else {
            onAdNotAvailable()
            preloadRewardedAd()
        }
    }

    /**
     * Check if interstitial ad is ready
     */
    fun isInterstitialReady(): Boolean = interstitialAd != null

    /**
     * Check if rewarded ad is ready
     */
    fun isRewardedReady(): Boolean = rewardedAd != null

    /**
     * Destroy ads
     */
    fun destroy() {
        interstitialAd = null
        rewardedAd = null
    }
}

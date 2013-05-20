package com.applifier.impact.android.banner;

/**
 * Listener for the ApplifeirImpactBanner
 * 
 * @author tuomasrinta
 *
 */
public interface IApplifierImpactBannerListener {
	
	public void onBannerAvailable();
	public void onBannerNotAvailable();
	public boolean onBannerClicked();
	
}

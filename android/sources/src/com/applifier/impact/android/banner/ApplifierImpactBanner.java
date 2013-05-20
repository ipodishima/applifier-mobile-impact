package com.applifier.impact.android.banner;

import java.util.HashMap;

import com.applifier.impact.android.ApplifierImpact;
import com.applifier.impact.android.IApplifierImpactListener;
import com.applifier.impact.android.properties.ApplifierImpactProperties;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * The ApplifierImpactBanner is used to display a banner on-screen that matches common mobile ad unit (for example the
 * AdMob ad unit) sizes, and promotes the Impact offer to the user.
 * 
 * @author tuomasrinta
 *
 */
public class ApplifierImpactBanner extends LinearLayout implements IApplifierImpactListener {
	
	public static final int UNIT_WIDTH = 480;
	public static final int UNIT_HEIGHT = 60;
	
	// The instance of our current Impact
	private ApplifierImpact impactInstance = null;
	
	// The original impact listener
	private IApplifierImpactListener originalListener = null;
	
	// The WebView that shows the banner content
	private WebView webView = null;
	
	// Is the webview content loaded?
	private boolean webViewContentLoaded = false;
	
	// Is the banner initialized 
	private boolean bannerInitialized = false;
	
	// Is the banner currently shown to the user
	private boolean bannerVisible = false;
	
	// Banner listener
	private IApplifierImpactBannerListener bannerListener = null;
	
	// Pre-open state
	private boolean isOpenBeforeImpact = false; 

	/**
	 * Creates the ApplifierImpactBanner
	 * @param ctx
	 */
	public ApplifierImpactBanner(Context ctx, IApplifierImpactBannerListener listener) {
		super(ctx);
		
		Log.d("applifier_banner", "Starting...");
		
		this.setBannerListener(listener);
		
		// We need to check if this is null, or not
		this.impactInstance = ApplifierImpact.instance;
		if(this.impactInstance == null) {
			throw new IllegalStateException("Initialize ApplifierImpact before creating the ApplifierImpactBanner");
		}
		
		// See if Impact has already initialized, if not, grab the listener and 
		// wait for it to tell us that it's done. 
		// As ApplifierImpact cannot handle multiple listeners in the current implementation, we need to replace
		// the developers listener with our own that proxies all the events
		if(!this.impactInstance.isCampaignDataInitialized()) {
			Log.d("applifier_banner", "setting listeners");
			this.originalListener = this.impactInstance.getImpactListener();
			this.impactInstance.setImpactListener(this);
		} else {
			Log.d("applifier_banner","Campaign data inited, lollerskatesd?");
			this.initializeBanner();
		}
		
		// Set the dimensions
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)this.getLayoutParams();
		if(params == null) {
			params = new LinearLayout.LayoutParams(UNIT_WIDTH, UNIT_HEIGHT);
		}
		params.width = UNIT_WIDTH;
		params.height = UNIT_HEIGHT;
		params.gravity = Gravity.CENTER_HORIZONTAL;
		
		this.setLayoutParams(params);

	}
	
	/**
	 * Set the IApplifierImpactBannerListener to be notified of banner changes
	 * 
	 * @param listener
	 */
	public void setBannerListener(IApplifierImpactBannerListener listener) {
		this.bannerListener = listener;
	}
	
	/**
	 * Shows the banner to the user
	 */
	private void initializeBanner() {
		Log.d("applifier_banner", "initializeBanner()");
		if(this.impactInstance.canShowCampaigns() && 
				ApplifierImpactProperties.BANNER_URL != null) {
			this.webView = new WebView(this.getContext());
			this.webView.setWebViewClient(new WebViewClient() {
				public void onPageCompleted() {
					ApplifierImpactBanner.this.webViewContentLoaded = true;
				}
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if("impact.show".equals(url)) {
						Log.d("applifier_banner", "impact.show URL");
						ApplifierImpactBanner.this.bannerClicked();
						return true;
					}
					return false;
				}
				@Override
				public void onLoadResource(WebView view, String url){
					Log.d("applifier_banner","onLoadResource");
					if("impact.show".equals(url)) {
						Log.d("applifier_banner", "impact.show in load resource");
						ApplifierImpactBanner.this.bannerClicked();
					}
			    }				
			});
			this.webView.setVisibility(View.GONE);
			this.webView.loadUrl(ApplifierImpactProperties.BANNER_URL);
			this.addView(this.webView);
			LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams)this.webView.getLayoutParams();
			llp.gravity = Gravity.CENTER_HORIZONTAL;
			llp.width = UNIT_WIDTH;
			llp.height = UNIT_HEIGHT;
			this.webView.setLayoutParams(llp);
			this.bannerInitialized = true;
			if(this.bannerListener != null) {
				this.bannerListener.onBannerAvailable();
			}
		} else {
			if(this.bannerListener != null) {
				Log.d("applifier_banner", "Calling bannerNotAvailable as initialize conditions not fulfilled");
				Log.d("applifier_banner", this.impactInstance.canShowCampaigns() + " " + (ApplifierImpactProperties.BANNER_URL != null));
				this.bannerListener.onBannerNotAvailable();
			}
		}
	}
	
	public void bannerClicked() {
		
		Log.d("applifier_banner", "bannerClicked()");
		
		if(this.bannerListener != null) {
			if(!this.bannerListener.onBannerClicked()) {
				Log.d("applifier_banner", "Listener handled click");
				return;
			}
		}
		
		if(this.impactInstance.canShowCampaigns()) {
			
			HashMap<String, Object> map = new HashMap<String,Object>();
			map.put(ApplifierImpact.APPLIFIER_IMPACT_OPTION_NOOFFERSCREEN_KEY, true);
			this.impactInstance.showImpact(map);
			
		}
	}
	
	/**
	 * Hide the current banner instance (if shown)
	 */
	public void hideBanner() {
		
		if(!this.bannerVisible) {
			throw new IllegalStateException("Cannot hide - banner not visible");
		}
		
		this.webView.setVisibility(View.GONE);
		this.bannerVisible = false;
		
	}
	
	
	/**
	 * Show the current banner instance. Returns true if showing succesfull, false if cannot
	 * be shown
	 */
	public boolean showBanner() {
		
		
		if(this.bannerVisible) {
			throw new IllegalStateException("Trying to show an already visible banner");
		}
		
		if(!this.bannerInitialized)
			return false;
		
		
		if(!this.impactInstance.canShowCampaigns() ||
				ApplifierImpactProperties.BANNER_URL == null) {
			return false;
		}
		
		this.webView.setVisibility(View.VISIBLE);
		this.bannerVisible = true;
		
		return true;
	}

	
	/**
	 * Listener methods to proxy the calls
	 */	
	
	@Override
	public void onImpactClose() {
		if(this.isOpenBeforeImpact) {
			// If there's still stuff to show and the banner was visible, put it back
			if(this.impactInstance.canShowCampaigns()) {
				this.showBanner();
			}
			// Reset to normal state
			this.isOpenBeforeImpact = false;
		}
		this.originalListener.onImpactClose();
	}

	@Override
	public void onImpactOpen() {
		if(this.bannerVisible) {
			this.isOpenBeforeImpact = true;
			this.hideBanner();
		}
		this.originalListener.onImpactOpen();
	}

	@Override
	public void onVideoStarted() {
		this.originalListener.onVideoStarted();
	}

	@Override
	public void onVideoCompleted(String rewardItemKey) {
		this.originalListener.onVideoCompleted(rewardItemKey);
	}

	@Override
	public void onCampaignsAvailable() {
		Log.d("applifier_banner", "onCampaignsAvailable");
		this.initializeBanner();
		this.originalListener.onCampaignsAvailable();
		
	}

	@Override
	public void onCampaignsFetchFailed() {
		this.originalListener.onCampaignsFetchFailed();
		if(this.bannerListener != null) {
			Log.d("applifier_banner", "Impact campaigns not available, calling bannerNotAvailable()");
			this.bannerListener.onBannerNotAvailable();
		}
	}
	
	
	
	

}

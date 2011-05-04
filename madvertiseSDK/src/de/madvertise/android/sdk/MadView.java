/*
 * Copyright 2011 madvertise Mobile Advertising GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.madvertise.android.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Ad View Container to show an ad 
 * 
 */
public class MadView extends LinearLayout {

	private static final boolean IS_TESTMODE_DEFAULT = false;

	// parameters for shine effect of the textview banner
	private final int GRADIENT_TOP_ALPHA = (int) (255 * 0.50);
	private final double GRADIENT_STOP = 0.7375;

	private Ad currentAd;
	private BitmapDrawable textBannerBackground;

	// parameters of the mad view
	private int textColor = MadUtil.TEXT_COLOR_DEFAULT;
	private int backgroundColor = MadUtil.BACKGROUND_COLOR_DEFAULT;
	private int secondsToRefreshAd = MadUtil.SECONDS_TO_REFRESH_AD_DEFAULT;
	private boolean testMode = IS_TESTMODE_DEFAULT;
	private String bannerType = MadUtil.BANNER_TYPE_DEFAULT;
	private boolean deliverOnlyText = MadUtil.DELIVER_ONLY_TEXT_DEFAULT;
	private int textSize = MadUtil.TEXT_SIZE_DEFAULT;
	private int bannerHeight = MadUtil.MMA_BANNER_HEIGHT_DEFAULT;
	
	private MadViewCallbackListener callbackListener = null;

	private Timer adTimer = null;

	private final Handler mHandler = new Handler();

	private boolean runningRefreshAd = false;
	
	private Drawable initialBackground = null;

	/**
	 * Constructor
	 * 
	 * @param context
	 */
	public MadView(Context context) {
		this(context, null);
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param attrs
	 */
	public MadView(Context context, AttributeSet attrs) {
		super(context, attrs);

		MadUtil.logMessage(null, Log.DEBUG, "** Constructor for mad view called **");
		setVisibility(INVISIBLE);

		if (context.checkCallingOrSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {

		    MadUtil.logMessage(null, Log.DEBUG, " *** ----------------------------- *** ");
		    MadUtil.logMessage(null, Log.DEBUG, " *** Missing internet permissions! *** ");
		    MadUtil.logMessage(null, Log.DEBUG, " *** ----------------------------- *** ");
			throw new IllegalArgumentException();
		}

		initParameters(attrs);

		
		Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		MadUtil.logMessage(null, Log.DEBUG, "Display values: Width = " + display.getWidth() + " ; Height = " + display.getHeight());

		setGravity(Gravity.CENTER);

		initialBackground = this.getBackground();
		Rect r = new Rect(0, 0, display.getWidth(), display.getHeight());
		textBannerBackground = generateBackgroundDrawable(r, backgroundColor, 0xffffff);

		setClickable(true);
		setFocusable(true);
		setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    MadUtil.logMessage(null, Log.DEBUG, "onTouchEvent(MotionEvent event) fired");
		if (currentAd != null) currentAd.handleClick();
		return true;
	}

	private void refreshView() {
		setBackgroundDrawable(initialBackground);
		
		if (currentAd != null) {
			if (currentAd.hasBanner() && !deliverOnlyText) {
				showStaticBannerView();
			} else {
				showTextBannerView();
			}
			notifyListener(true);
		} else {
			removeAllViews();
			notifyListener(false);
		}
	}

	private void showStaticBannerView() {
	    MadUtil.logMessage(null, Log.DEBUG, "Add static banner");

		Bitmap bannerBitmap = BitmapFactory.decodeByteArray(currentAd.getImageByteArray(), 0, currentAd.getImageByteArray().length);
		StaticBannerView staticBannerView = new StaticBannerView(getContext(), bannerBitmap);

		removeAllViews();
		addView(staticBannerView);
	}

	private void showTextBannerView() {
	    MadUtil.logMessage(null, Log.DEBUG, "Add text banner");
		TextView textView = new TextView(getContext());
		textView.setGravity(Gravity.CENTER);
		textView.setText(currentAd.getText());
		textView.setTextSize(textSize);
		textView.setTextColor(textColor);
		textView.setTypeface(Typeface.DEFAULT_BOLD);
		setBackgroundDrawable(textBannerBackground);

		removeAllViews();
		addView(textView);
	}

	/**
	 * Convenience method to notify the callback listener
	 * @param succeed
	 */
	private void notifyListener(boolean succeed) {
		if (callbackListener != null) {
			callbackListener.onLoaded(succeed, this);
		} else {
		    MadUtil.logMessage(null, Log.DEBUG, "Callback Listener not set");
		}
	}

	/**
	 * Set the visibility state of this view.
	 * 
	 * @param visibility - set the visibility with <code>VISIBLE</code>, <code>INVISIBLE</code> or <code>GONE</code>.
	 */
	@Override
	public void setVisibility(int visibility) {
		int originVisibility = super.getVisibility();

		if (originVisibility != visibility) {
			synchronized (this) {
				int childViewCounter = getChildCount();

				for (int i = 0; i < childViewCounter; i++) {
					View child = getChildAt(i);
					child.setVisibility(visibility);
				}
				super.setVisibility(visibility);
			}
		}
	}

	/**
	 * Reads all parameters, not needed for a request to the ad server (colors,
	 * refresh timeout, ...)
	 * 
	 * @param attrs
	 *            attribute set for the view
	 */
	private void initParameters(AttributeSet attrs) {
		if (attrs != null) {
			String packageName = "http://schemas.android.com/apk/res/" + getContext().getPackageName();
			if (packageName != null) {
			    MadUtil.logMessage(null, Log.DEBUG, "namespace = " + packageName);
			}
			testMode = attrs.getAttributeBooleanValue(packageName, "isTestMode", IS_TESTMODE_DEFAULT);
			textColor = attrs.getAttributeIntValue(packageName, "textColor", MadUtil.TEXT_COLOR_DEFAULT);
			backgroundColor = attrs.getAttributeIntValue(packageName, "backgroundColor", MadUtil.BACKGROUND_COLOR_DEFAULT);
			secondsToRefreshAd = attrs.getAttributeIntValue(packageName, "secondsToRefresh", MadUtil.SECONDS_TO_REFRESH_AD_DEFAULT);
			bannerType = attrs.getAttributeValue(packageName, "bannerType");
			if (bannerType == null) bannerType = MadUtil.BANNER_TYPE_DEFAULT;
			deliverOnlyText = attrs.getAttributeBooleanValue(packageName, "deliverOnlyText", MadUtil.DELIVER_ONLY_TEXT_DEFAULT);
			textSize = attrs.getAttributeIntValue(packageName, "textSize", MadUtil.TEXT_SIZE_DEFAULT);
		} else {
		    MadUtil.logMessage(null, Log.DEBUG, "AttributeSet is null!");
		}

		if (secondsToRefreshAd != 0 && secondsToRefreshAd < 60) secondsToRefreshAd = MadUtil.SECONDS_TO_REFRESH_AD_DEFAULT;
		
		if (bannerType.equals("iab")) bannerHeight = MadUtil.IAB_BANNER_HEIGHT_DEFAULT; 
		
		MadUtil.logMessage(null, Log.DEBUG, "Using following attributes values:");
		MadUtil.logMessage(null, Log.DEBUG, " testMode = " + testMode);
		MadUtil.logMessage(null, Log.DEBUG, " textColor = " + textColor);
		MadUtil.logMessage(null, Log.DEBUG, " backgroundColor = " + backgroundColor);
		MadUtil.logMessage(null, Log.DEBUG, " secondsToRefreshAd = " + secondsToRefreshAd);
		MadUtil.logMessage(null, Log.DEBUG, " bannerType = " + bannerType);
		MadUtil.logMessage(null, Log.DEBUG, " deliverOnlyText = " + deliverOnlyText);
		MadUtil.logMessage(null, Log.DEBUG, " textSize = " + textSize);
		MadUtil.logMessage(null, Log.DEBUG, " bannerHeight = " + bannerHeight);
	}

	/**
	 * Starts a background thread to fetch a new ad. Method is called
	 * from the refresh timer task
	 */
	private void requestNewAd() {
	    MadUtil.logMessage(null, Log.DEBUG, "Trying to fetch a new ad");

		// exit if already requesting a new ad, not used yet
		if (runningRefreshAd) {
		    MadUtil.logMessage(null, Log.DEBUG, "Another request is still in progress ...");
			return;
		}
		
		new Thread() {
			public void run() {
				// read all parameters, that we need for the request
				// get site token from manifest xml file
				String siteToken = MadUtil.getToken(getContext());
				if (siteToken == null) {
					siteToken = "";
					MadUtil.logMessage(null, Log.DEBUG, "Cannot show ads, since the appID ist null");
				} else {
				    MadUtil.logMessage(null, Log.DEBUG, "appID = " + siteToken);
				}

				// get uid (does not work in emulator)
				String uid = Secure.getString(getContext().getContentResolver(), Secure.ANDROID_ID);
				if (uid == null) {
					uid = "";
				} else {
					uid = getMD5Hash(uid);
				}
				MadUtil.logMessage(null, Log.DEBUG, "uid = " + uid);

				// get display metrics
				Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				int displayHeight = display.getHeight();
				int displayWidth = display.getWidth();
				MadUtil.logMessage(null, Log.DEBUG, "Display height = " + Integer.toString(displayHeight));
				MadUtil.logMessage(null, Log.DEBUG, "Display width = " + Integer.toString(displayWidth));

				// create post request
				HttpPost postRequest = new HttpPost(MadUtil.MAD_SERVER + "/site/" + siteToken);
				postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

				List<NameValuePair> parameterList = new ArrayList<NameValuePair>();
				parameterList.add(new BasicNameValuePair("ua" , MadUtil.getUA()));
				parameterList.add(new BasicNameValuePair("app", "true"));
				parameterList.add(new BasicNameValuePair("debug", Boolean.toString(testMode)));
				parameterList.add(new BasicNameValuePair("ip", MadUtil.getLocalIpAddress()));
				parameterList.add(new BasicNameValuePair("format", "json"));
				parameterList.add(new BasicNameValuePair("requester", "android_sdk"));
				parameterList.add(new BasicNameValuePair("version", "1.1"));
				parameterList.add(new BasicNameValuePair("uid", uid));
				parameterList.add(new BasicNameValuePair("banner_type", bannerType));
				parameterList.add(new BasicNameValuePair("deliver_only_text", Boolean.toString(deliverOnlyText)));

				MadUtil.refreshCoordinates(getContext());
				if (MadUtil.getLocation() != null) {
					parameterList.add(new BasicNameValuePair("lat", Double.toString(MadUtil.getLocation().getLatitude())));
					parameterList.add(new BasicNameValuePair("lng", Double.toString(MadUtil.getLocation().getLongitude())));
				}

				UrlEncodedFormEntity urlEncodedEntity = null;
				try {
					urlEncodedEntity = new UrlEncodedFormEntity(parameterList);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				postRequest.setEntity(urlEncodedEntity);

				MadUtil.logMessage(null, Log.DEBUG, "Post request created");
				MadUtil.logMessage(null, Log.DEBUG, "Uri : " + postRequest.getURI().toASCIIString());
				MadUtil.logMessage(null, Log.DEBUG, "All headers : " + MadUtil.getAllHeadersAsString(postRequest.getAllHeaders()));
				MadUtil.logMessage(null, Log.DEBUG, "All request parameters :" + MadUtil.printRequestParameters(parameterList));

				synchronized (this) {
					// send blocking request to ad server
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = null;
					InputStream inputStream = null;
					boolean jsonFetched = false;
					JSONObject json = null;

					try {
						HttpParams clientParams = httpClient.getParams();
						HttpConnectionParams.setConnectionTimeout(clientParams, MadUtil.CONNECTION_TIMEOUT.intValue());
						HttpConnectionParams.setSoTimeout(clientParams, MadUtil.CONNECTION_TIMEOUT.intValue());

						MadUtil.logMessage(null, Log.DEBUG, "Sending request");
						httpResponse = httpClient.execute(postRequest);

						MadUtil.logMessage(null, Log.DEBUG, "Response Code => " + httpResponse.getStatusLine().getStatusCode());
						if (testMode)
							MadUtil.logMessage(null, Log.DEBUG, "Madvertise Debug Response: " + httpResponse.getLastHeader("X-Madvertise-Debug"));
						int responseCode = httpResponse.getStatusLine().getStatusCode();

						HttpEntity entity = httpResponse.getEntity();

						if (responseCode == 200 && entity != null) {
							inputStream = entity.getContent();
							String resultString = MadUtil.convertStreamToString(inputStream);
							MadUtil.logMessage(null, Log.DEBUG, "Response => " + resultString);
							json = new JSONObject(resultString);
							jsonFetched = true;
						}
					} catch (ClientProtocolException e) {
						e.printStackTrace();
						MadUtil.logMessage(null, Log.DEBUG, "Error in HTTP request / protocol");
					} catch (IOException e) {
						e.printStackTrace();
						MadUtil.logMessage(null, Log.DEBUG, "Could not receive a http response on an ad reqeust");
					} catch (JSONException e) {
						e.printStackTrace();
						MadUtil.logMessage(null, Log.DEBUG, "Could not parse json object");
					} finally {
						if (inputStream != null)
							try {
								inputStream.close();
							} catch (IOException e) {
							}
					}

					// create ad, this is a blocking call
					if (jsonFetched) {
						currentAd = new Ad(getContext(), json);
					}
				}
				mHandler.post(mUpdateResults);

			}
		}.start();
	}

	// used for execution in the ui main thread
	private final Runnable mUpdateResults = new Runnable() {
		public void run() {
			refreshView();
		}
	};

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
	    MadUtil.logMessage(null, Log.DEBUG, "#### onWindowFocusChanged fired ####");
		refreshAdTimer(hasWindowFocus);
		super.onWindowFocusChanged(hasWindowFocus);
	}

	@Override
	protected void onAttachedToWindow() {
	    MadUtil.logMessage(null, Log.DEBUG, "#### onAttachedToWindow fired ####");
		refreshAdTimer(true);
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
	    MadUtil.logMessage(null, Log.DEBUG, "#### onDetachedFromWindow fired ####");
		refreshAdTimer(false);
		super.onDetachedFromWindow();
	}

	/**
	 * Convenicence method for a shiny background for text ads
	 * 
	 * @param rect
	 * @param backgroundColor
	 * @param textColor
	 * @return
	 */
	private BitmapDrawable generateBackgroundDrawable(Rect rect, int backgroundColor, int shineColor) {
		try {
			Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawTextBannerBackground(canvas, rect, backgroundColor, shineColor);
			return new BitmapDrawable(bitmap);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Draw the ad background for a text banner
	 * 
	 * @param canvas
	 * @param rectangle
	 * @param backgroundColor
	 * @param textColor
	 */
	private void drawTextBannerBackground(Canvas canvas, Rect rectangle, int backgroundColor, int shineColor) {
		Paint paint = new Paint();
		paint.setColor(backgroundColor);
		paint.setAntiAlias(true);
		canvas.drawRect(rectangle, paint);

		int upperColor = Color.argb(GRADIENT_TOP_ALPHA, Color.red(shineColor), Color.green(shineColor), Color.blue(shineColor));
		int[] gradientColors = { upperColor, shineColor };
		GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors);

		int stop = (int) (rectangle.height() * GRADIENT_STOP) + rectangle.top;
		gradientDrawable.setBounds(rectangle.left, rectangle.top, rectangle.right, stop);
		gradientDrawable.draw(canvas);

		Rect shadowRect = new Rect(rectangle.left, stop, rectangle.right, rectangle.bottom);
		Paint shadowPaint = new Paint();
		shadowPaint.setColor(shineColor);
		canvas.drawRect(shadowRect, shadowPaint);
	}

	/**
	 * Handles the refresh timer
	 * @param starting
	 */
	private void refreshAdTimer(boolean starting) {
		synchronized (this) {
			if (starting) {
				if (adTimer == null) {
					adTimer = new Timer();
					adTimer.schedule(new TimerTask() {
						public void run() {
						    MadUtil.logMessage(null, Log.DEBUG, "Refreshing ad ...");
							requestNewAd();
						}
					}, (long) 0, (long) secondsToRefreshAd * 1000);
				}
			} else {
				if (adTimer != null) {
				    MadUtil.logMessage(null, Log.DEBUG, "Stopping refresh timer ...");
					adTimer.cancel();
					adTimer = null;
				}
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasuredWidth();
		setMeasuredDimension(width, bannerHeight);
	}

	/**
	 * Returns the MD5 hash for a string.
	 * 
	 * @param input
	 * @return md5 hash
	 */
	private String getMD5Hash(String input) {
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			MadUtil.logMessage(null, Log.DEBUG, "Could not create hash value");
			return "";
		}
		messageDigest.update(input.getBytes());
		String temp = null;
		byte[] digest = messageDigest.digest();
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			temp = Integer.toHexString(0xFF & digest[i]);
			if (temp.length() < 2) {
				temp = "0" + temp;
			}
			hexString.append(temp);
		}
		return hexString.toString();
	}

	
	/**
	 * Removes the current listener that receives notifications about the ad loading process	
	 */
	public void removeMadViewCallbackListener() {
		callbackListener = null;
	}
	
	/**
	 * Returns the current listener that receives notifications about the ad loading process
	 * 
	 * @return
	 */
	public MadViewCallbackListener getCallbackListener() {
		return callbackListener;
	}

	/**
	 * Sets a listener that receives notifications about the ad loading process
	 * @param listener
	 */
	public void setMadViewCallbackListener(MadViewCallbackListener listener) {
		callbackListener = listener;
	}

	/**
	 * Interface to receive a callback, if the ad loading was successful or not
	 */
	public interface MadViewCallbackListener {
		/**
		 * Notifies the listener on success or failure
		 * 
		 * @param succeed
		 *            true, if an ad could be loading, else false
		 * @param madView
		 *            specified view
		 */
		public void onLoaded(boolean succeed, MadView madView);
	}
}
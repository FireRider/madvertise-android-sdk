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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.NameValuePair;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Utility class for the madvertise android SDK.
 */
public class MadUtil {

	protected static final String LOG = "MAD_LOG";
	protected static final boolean PRINT_LOG = true;
	protected static final String MAD_SERVER = "http://ad.madvertise.de";
//	protected static final String MAD_SERVER = "http://10.0.0.138:9292";
	protected static final Integer CONNECTION_TIMEOUT = new  Integer(1000);
	protected static final String ENCODING = "UTF-8";
	protected static final int SECONDS_TO_REFRESH_LOCATION = 900;
	protected static final int SECONDS_TO_REFRESH_AD_DEFAULT = 30;
	
	protected static final int TEXT_COLOR_DEFAULT = 0xffffffff;
	protected static final int BACKGROUND_COLOR_DEFAULT = 0x000000;
	protected static final String BANNER_TYPE_DEFAULT = "mma";
	protected static final boolean DELIVER_ONLY_TEXT_DEFAULT = false;
	protected static final int TEXT_SIZE_DEFAULT = 18;
	
	protected static final int MMA_BANNER_HEIGHT_DEFAULT = 53;
	protected static final int IAB_BANNER_HEIGHT_DEFAULT = 250;
	
	private static String UA;
	private static long locationUpdateTimestamp = 0;
	private static Location currentLocation = null;
	
	/**
	 * Returns the madvertise token
	 * 
	 * @param context
	 *            application context
	 * @return madvertise_token from AndroidManifest.xml or null
	 */
	protected static String getToken(Context context) {
		String madvertiseToken = null;

		PackageManager packageManager = context.getPackageManager();
		try {
			ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			madvertiseToken = applicationInfo.metaData.getString("madvertise_site_token");
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (madvertiseToken == null) {
			if (PRINT_LOG) Log.d(MadUtil.LOG, "Could not fetch \"madvertise_site_token\" from AndroidManifest.xml");
			
		}

		return madvertiseToken;
	}

	/**
	 * Fetch the address of the enabled interface
	 * 
	 * @return ip address as string
	 */
	protected static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			if (PRINT_LOG) Log.d(MadUtil.LOG, ex.toString());
		}
		return "";
	}

	/**
	 * Print all header parameters, just for logging purpose
	 * 
	 * @param headers
	 *            header object
	 * @return all headers concatenated
	 */
	protected static String getAllHeadersAsString(Header[] headers) {
		String returnString = "";
		for (int i = 0; i < headers.length; i++) {
			returnString += "<< " + headers[i].getName() + " : " + headers[i].getValue() + " >>";
		}
		return returnString;
	}

	/**
	 * Converts an input stream into a byte array
	 * 
	 * @param is
	 *            the input stream
	 * @return byte array
	 */
	protected static byte[] convertStreamToByteArray(InputStream inputStream) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
		byte[] buffer = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buffer)) >= 0) {
				byteArrayOutputStream.write(buffer, 0, len);
			}
		} catch (IOException e) {
		}
		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Compares two streams
	 * 
	 * @param inputStream1
	 * @param inputStream2
	 * @return true, if streams are the same
	 * @throws IOException
	 */
	protected static boolean compareTwoStreams(InputStream inputStream1, InputStream inputStream2) throws IOException {
		boolean error = false;
		try {
			byte[] buffer1 = new byte[1024];
			byte[] buffer2 = new byte[1024];
			try {
				int numRead1 = 0;
				int numRead2 = 0;
				while (true) {
					numRead1 = inputStream1.read(buffer1);
					numRead2 = inputStream2.read(buffer2);
					if (numRead1 > -1) {
						if (numRead2 != numRead1)
							return false;
						if (!Arrays.equals(buffer1, buffer2))
							return false;
					} else {
						return numRead2 < 0;
					}
				}
			} finally {
				inputStream1.close();
			}
		} catch (IOException e) {
			error = true;
			throw e;
		} catch (RuntimeException e) {
			error = true;
			throw e;
		} finally {
			try {
				inputStream2.close();
			} catch (IOException e) {
				if (!error)
					throw e;
			}
		}
	}

	/**
	 * converts a stream to a string
	 * 
	 * @param inputStream stream from the http connection with the ad server
	 * @return json string from the ad server
	 */
	protected static String convertStreamToString(InputStream inputStream) {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return stringBuilder.toString();
	}

	protected static Location getLocation() {
		return currentLocation;
	}

	/**
	 * Try to update current location. Non blocking call.
	 * 
	 * @param context
	 *            application context
	 */
	protected static void refreshCoordinates(Context context) {
		if (PRINT_LOG) Log.d(LOG, "Trying to refresh location");
		
		if (context == null) {
			if (PRINT_LOG) Log.d(LOG, "Context not set - quit location refresh");
			return;
		}
		
		// check if we need a regular update
		if ((locationUpdateTimestamp + MadUtil.SECONDS_TO_REFRESH_LOCATION * 1000) > System.currentTimeMillis()) {
			if (PRINT_LOG) Log.d(LOG, "It's not time yet for refreshing the location");
			return;
		}
		
		synchronized (context) {
			// recheck, if location was updated by another thread while we paused
			if ((locationUpdateTimestamp + MadUtil.SECONDS_TO_REFRESH_LOCATION * 1000) > System.currentTimeMillis()) {
				if (PRINT_LOG) Log.d(LOG, "Another thread updated the loation already");
				return;
			}
			
			boolean permissionCoarseLocation = context.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			boolean permissionFineLocation = context.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	
			// return (null) if we do not have any permissions
			if (!permissionCoarseLocation && !permissionFineLocation) {
				if (PRINT_LOG) Log.d(LOG, "No permissions for requesting the location");
				return;
			}
	
			// return (null) if we can't get a location manager
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			if (locationManager == null) {
				if (PRINT_LOG) Log.d(LOG, "Unable to fetch a location manger");
				return;
			}
			
			String provider = null;
			Criteria criteria = new Criteria();
			criteria.setCostAllowed(false);
	
			// try to get coarse location first
			if (permissionCoarseLocation) {
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
				provider = locationManager.getBestProvider(criteria, true);
			}
	
			// try to get gps location if coarse locatio did not work
			if (provider == null && permissionFineLocation) {
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				provider = locationManager.getBestProvider(criteria, true);
			}
	
			// still no provider, return (null)
			if (provider == null) {
				if (PRINT_LOG) Log.d(LOG, "Unable to fetch a location provider");
				return;
			}
	
			// create a finalized reference to the location manager, in order to
			// access it in the inner class
			final LocationManager finalizedLocationManager = locationManager;
			locationUpdateTimestamp = System.currentTimeMillis();
			locationManager.requestLocationUpdates(provider, 0, 0, new LocationListener() {
				public void onLocationChanged(Location location) {
					if (PRINT_LOG) Log.d(LOG, "Refreshing location");
					currentLocation = location;
					locationUpdateTimestamp = System.currentTimeMillis();
					// stop draining battery life
					finalizedLocationManager.removeUpdates(this);
				}
				// not used yet
				public void onProviderDisabled(String provider) {}
				public void onProviderEnabled(String provider) {}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
			}, context.getMainLooper());
		}
	}

	protected static String printRequestParameters(List<NameValuePair> parameterList) {
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<NameValuePair> nameValueIterator = parameterList.iterator();
		
		while (nameValueIterator.hasNext()) {
			NameValuePair pair = nameValueIterator.next();
			stringBuilder.append(pair.getName() + "=" + pair.getValue() + "||" );
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * Generate a User-Agent used in HTTP request to pick an ad.
	 * Source used from Android source code "frameworks/base/core/java/android/webkit/WebSettings.java"
	 * 
	 * @return
	 */
	protected static String getUA() {
		if (UA != null) return UA;
		
		StringBuffer arg = new StringBuffer();
		
		final String version = Build.VERSION.RELEASE;
		if (version.length() > 0) {
			arg.append(version);
		} else {
			arg.append("1.0");
		}
		arg.append("; ");

		final Locale l = Locale.getDefault();
		final String language = l.getLanguage();
		if (language != null) {
			arg.append(language.toLowerCase());
			final String country = l.getCountry();
			if (country != null) {
				arg.append("-");
				arg.append(country.toLowerCase());
			}
		} else {
			arg.append("de");
		}
		final String model = Build.MODEL;
		if (model.length() > 0) {
			arg.append("; ");
			arg.append(model);
		}
		final String id = Build.ID;
		if (id.length() > 0) {
			arg.append(" Build/");
			arg.append(id);
		}
		
		// TODO: add version detection for AppleWebKit, Version and Safari
		final String rawUA = "Mozilla/5.0 (Linux; U; Android %s) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2";
		UA = String.format(rawUA, arg);
		
		return UA;
	}
	
	/**
	 * Simple logging helper to prevent producing duplicate code blocks.
	 * 
	 * Log-Message is only printed to LogCat if logging is enabled in MadUtils
	 * and message is logable with specified tag and level. 
	 *  
	 * @param tag
	 *     use a given tag for logging or use default tag if nil. Default tag can be defined in MadUtil class.
	 * @param level
	 *     log level from {@link android.util.Log}
	 * @param message
	 * @see 
	 *     android.util.Log
	 */
	protected static void logMessage(String tag, int level, String message) {
	    if(!PRINT_LOG) {
            if (!Log.isLoggable(tag, level))
                return;
        }
	    if(tag == null) tag = MadUtil.LOG;
	    Log.println(level, tag, message);
	}
}
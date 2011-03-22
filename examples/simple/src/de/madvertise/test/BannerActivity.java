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
package de.madvertise.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.madvertise.android.sdk.MadView;
import de.madvertise.android.sdk.MadView.MadViewCallbackListener;

/**
 * BannerActivity.java
 * 
 * Example activity that shows how the madvertise SDK can be integrated.
 * 
 * It shows a list view containing some countries and the madvertise banner
 * at the top. The integration of the madvertise is done in the layout xml file.
 */
public class BannerActivity extends Activity implements MadViewCallbackListener {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// set the layout
		setContentView(R.layout.main);
		
		// set the callback listener, to receive a message when an ad was loaded
		MadView madView = (MadView) findViewById(R.id.madad);
		madView.setMadViewCallbackListener(this);
		
		// prepare the list adapter with some countries
		String[] countries = new String[] { "French Southern Territories", "Gabon", "Georgia", "Germany", "Ghana", "Gibraltar", "Greece", "Greenland", "Grenada", "Guadeloupe",
				"Guam", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Heard Island and McDonald Islands", "Honduras", "Hong Kong", "Hungary", "Iceland", "India",
				"Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos" };
		ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.list_item, countries);
		
		// get list reference from layout file for action and adapter settings
		ListView listView = (ListView) findViewById(R.id.country_list);
		
		// show a small popup, when an item is clicked
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(getApplicationContext(), ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
			}
		});

		// add countries to athe list
		listView.setAdapter(adapter);
	}

	
	/**
	 * Notifies the listener on success or failure
	 * 
	 * @param succeed
	 *            true, if an ad could be loading, else false
	 * @param madView
	 *            specified view
	 */
	public void onLoaded(boolean succeed, MadView madView) {
		if (succeed) {
			// ad loaded, set view visible
			Log.d("YOUR_LOG_TAG", "Ad successfully loaded");
			madView.setVisibility(View.VISIBLE);
		} else {
			// ad could not be loaded, set view to invisible
			Log.w("YOUR_LOG_TAG", "Ad could not be loaded");
			madView.setVisibility(View.INVISIBLE);
		}
	}
}
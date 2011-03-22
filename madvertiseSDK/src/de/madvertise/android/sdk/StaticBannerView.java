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

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * Simple view to show a non animated ad. 
 */
public class StaticBannerView extends ImageView {
	
	public StaticBannerView(Context context, Bitmap bannerBitmap) {
		super(context);
		if (bannerBitmap != null) {
			this.setImageBitmap(bannerBitmap);
		}
	}
}
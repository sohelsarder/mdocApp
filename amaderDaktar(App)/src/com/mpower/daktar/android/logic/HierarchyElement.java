/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mpower.daktar.android.logic;

import java.util.ArrayList;

import org.javarosa.core.model.FormIndex;

import android.graphics.drawable.Drawable;

public class HierarchyElement {
	private String mPrimaryText = "";
	private String mSecondaryText = "";
	private Drawable mIcon;
	private int mColor;
	int mType;
	FormIndex mFormIndex;
	ArrayList<HierarchyElement> mChildren;

	public HierarchyElement(final String text1, final String text2,
			final Drawable bullet, final int color, final int type,
			final FormIndex f) {
		mIcon = bullet;
		mPrimaryText = text1;
		mSecondaryText = text2;
		mColor = color;
		mFormIndex = f;
		mType = type;
		mChildren = new ArrayList<HierarchyElement>();
	}

	public String getPrimaryText() {
		return mPrimaryText;
	}

	public String getSecondaryText() {
		return mSecondaryText;
	}

	public void setPrimaryText(final String text) {
		mPrimaryText = text;
	}

	public void setSecondaryText(final String text) {
		mSecondaryText = text;
	}

	public void setIcon(final Drawable icon) {
		mIcon = icon;
	}

	public Drawable getIcon() {
		return mIcon;
	}

	public FormIndex getFormIndex() {
		return mFormIndex;
	}

	public int getType() {
		return mType;
	}

	public void setType(final int newType) {
		mType = newType;
	}

	public ArrayList<HierarchyElement> getChildren() {
		return mChildren;
	}

	public void addChild(final HierarchyElement h) {
		mChildren.add(h);
	}

	public void setChildren(final ArrayList<HierarchyElement> children) {
		mChildren = children;
	}

	public void setColor(final int color) {
		mColor = color;
	}

	public int getColor() {
		return mColor;
	}

}

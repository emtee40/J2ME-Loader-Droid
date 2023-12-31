/*
 * Copyright 2018 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mascotcapsule.micro3d.v3;

import android.opengl.Matrix;

import javax.microedition.lcdui.Canvas;

@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
public class FigureLayout {

	private AffineTrans[] mAffineArray;
	private AffineTrans mAffineNow;
	private int mScaleX;
	private int mScaleY;
	private int mCenterX;
	private int mCenterY;
	private int mParaWidth;
	private int mParaHeight;
	private int mPersNear;
	private int mPersFar;
	private int mPersAngle;
	private int mPersWidth;
	private int mPersHeight;
	private int mSettingIndex;
	private int offsetX;
	private int offsetY;
	private int halfWidth, halfHeight;

	private float[] glMVPMatrix = new float[16];
	private float[] glProjectionMatrix = new float[16];
	private float[] mTempMatrix = new float[48];
	private float[] glCenter = new float[2];

	public FigureLayout() {
		this(null, 512, 512, 0, 0);
	}

	public FigureLayout(AffineTrans trans, int sx, int sy, int cx, int cy) {
		prepareMatrices();
		setAffineTrans(trans);
		setScale(sx, sy);
		setCenter(cx, cy);
		halfWidth = Canvas.getVirtualWidth() / 2;
		halfHeight = Canvas.getVirtualHeight() / 2;
	}

	public final AffineTrans getAffineTrans() {
		return this.mAffineNow;
	}

	public final void setAffineTrans(AffineTrans[] trans) {
		if (trans == null || trans.length == 0) {
			throw new NullPointerException();
		}
		for (AffineTrans tran : trans) {
			if (tran == null) throw new NullPointerException();
		}
		this.mAffineArray = trans;
	}

	/**
	 * Sets the affine transformation object.
	 *
	 * @param trans Affine transformation (no transformation if null)
	 */
	public final void setAffineTrans(AffineTrans trans) {
		if (trans == null) {
			trans = new AffineTrans();
			trans.setIdentity();
		}
		if (this.mAffineArray == null) {
			this.mAffineArray = new AffineTrans[1];
			this.mAffineArray[0] = trans;
		}
		this.mAffineNow = trans;
	}

	public final void setAffineTransArray(AffineTrans[] trans) {
		setAffineTrans(trans);
	}

	public final void selectAffineTrans(int idx) {
		if (this.mAffineArray == null || idx < 0 || idx >= this.mAffineArray.length) {
			throw new IllegalArgumentException();
		}
		this.mAffineNow = this.mAffineArray[idx];
	}

	public final int getScaleX() {
		return this.mScaleX;
	}

	public final int getScaleY() {
		return this.mScaleY;
	}

	public final void setScale(int sx, int sy) {
		this.mScaleX = sx;
		this.mScaleY = sy;
		this.mSettingIndex = 0;
		Matrix.setIdentityM(glProjectionMatrix, 0);
		Matrix.scaleM(glProjectionMatrix, 0, sx / 512F, sy / 512F, 1.0F);
	}

	public final int getParallelWidth() {
		return this.mParaWidth;
	}

	public final int getParallelHeight() {
		return this.mParaHeight;
	}

	public final void setParallelSize(int w, int h) {
		if (w < 0 || h < 0) {
			throw new IllegalArgumentException();
		}
		if (w == 0 || h == 0) {
			w = halfWidth * 2;
			h = halfHeight * 2;
		}
		this.mParaWidth = w;
		this.mParaHeight = h;
		this.mSettingIndex = 1;
		Matrix.orthoM(glProjectionMatrix, 0, -w / 2, w / 2, -h / 2, h / 2, 1, 4096);
	}

	public final int getCenterX() {
		return this.mCenterX;
	}

	public final int getCenterY() {
		return this.mCenterY;
	}

	public final void setCenter(int cx, int cy) {
		this.mCenterX = cx;
		this.mCenterY = cy;
		updateGlCenter();
	}

	public final void setPerspective(int zNear, int zFar, int angle) {
		if (zNear >= zFar || zNear < 1 || zNear > 32766 || zFar < 2 || zFar > 32767
				|| angle < 1 || angle > 2047) {
			throw new IllegalArgumentException();
		}
		this.mPersNear = zNear;
		this.mPersFar = zFar;
		this.mPersAngle = angle;
		this.mSettingIndex = 2;
		Matrix.perspectiveM(glProjectionMatrix, 0, angle * 480F / 4096F, (float) halfWidth / halfHeight, zNear, zFar);
	}

	public final void setPerspective(int zNear, int zFar, int width, int height) {
		if (zNear >= zFar || zNear < 1 || zNear > 32766 || zFar < 2 || zFar > 32767
				|| width < 0 || height < 0) {
			throw new IllegalArgumentException();
		}
		this.mPersNear = zNear;
		this.mPersFar = zFar;
		this.mPersWidth = width;
		this.mPersHeight = height;
		this.mSettingIndex = 3;
		Matrix.frustumM(glProjectionMatrix, 0, -width / 8192f, width / 8192f, -height / 8192f, height / 8192f, zNear, zFar);
	}

	public float[] getMatrix() {
		AffineTrans a = mAffineNow;
		float[] tmp = this.mTempMatrix;
		tmp[16] = a.m00; tmp[20] =  a.m01; tmp[24]  =  a.m02; tmp[12] =  a.m03;
		tmp[17] = a.m10; tmp[21] =  a.m11; tmp[25]  =  a.m12; tmp[13] =  a.m13;
		tmp[18] = a.m20; tmp[22] =  a.m21; tmp[26]  =  a.m22; tmp[14] = -a.m23;
		tmp[19] =  0.0F; tmp[23] =   0.0F; tmp[27]  =   0.0F; tmp[15] =   1.0F;
		Matrix.multiplyMV(tmp, 0, tmp, 32, tmp, 16);
		Matrix.multiplyMV(tmp, 4, tmp, 32, tmp, 20);
		Matrix.multiplyMV(tmp, 8, tmp, 32, tmp, 24);
		Matrix.multiplyMM(glMVPMatrix, 0, glProjectionMatrix, 0, tmp, 0);
		return glMVPMatrix;
	}

	private void prepareMatrices() {
		Matrix.setIdentityM(glMVPMatrix, 0);
		Matrix.setIdentityM(glProjectionMatrix, 0);
		float[] tmp = mTempMatrix;
		Matrix.setIdentityM(tmp, 32);
		Matrix.scaleM(tmp, 32, -1F/4096F, 1F/4096F, 1F/4096F);
		Matrix.rotateM(tmp, 32, 180, 0, 1, 0);
	}

	public void setOffset(int offsetX, int offsetY) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		updateGlCenter();
	}

	public void updateGlCenter() {
		glCenter[0] = (mCenterX - halfWidth + offsetX) / (float) halfWidth;
		glCenter[1] = (mCenterY - halfHeight + offsetY) / (float) halfHeight;
	}

	public float[] getGlCenter() {
		return glCenter;
	}

}

/*
 * Copyright (C) 2019 CW Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.audio7.util;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;

class JsonAsync extends AsyncTask <URL,Void,String> //Generic: Params, Progress, Result
{
	String title="";

    @Override
    protected void onPreExecute(){
    }

    @Override
    protected String doInBackground(URL... arg0) {
    	try {
			title = new JSONObject(IOUtils.toString(arg0[0])).getString("title");
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return title;
    }

    @Override
    protected void onPostExecute(String result) {
    	System.out.println("JsonAsync / _onPostExecute / result (title)= " + result);
		if(!this.isCancelled())
		{
			this.cancel(true);
		}
    }
}
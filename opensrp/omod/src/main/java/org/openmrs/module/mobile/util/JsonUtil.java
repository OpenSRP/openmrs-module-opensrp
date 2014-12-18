/**
 * This class reads and writes JSON objects/arrays
 */

package org.openmrs.module.mobile.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openmrs.module.mobile.util.JsonUtil;

/**
 * @author owais.hussain@irdresearch.org
 * 
 */
public class JsonUtil
{
	public static JSONObject getJSONObject (String jsonText)
	{
		// try parse the string to a JSON object
		try
		{
			JSONObject jsonObj = new JSONObject (jsonText);
			return jsonObj;
		}
		catch (JSONException e)
		{
			e.printStackTrace ();
			return null;
		}
	}

	public static JSONObject[] getJSONArrayFromObject (JSONObject jsonObj, String arrayElement)
	{
		try
		{
			JSONArray jsonArray = jsonObj.getJSONArray (arrayElement);
			JSONObject[] jsonObjects = new JSONObject[jsonArray.length ()];
			for (int i = 0; i < jsonArray.length (); i++)
			{
				jsonObjects[i] = JsonUtil.getJSONObject (jsonArray.getString (i));
			}
			return jsonObjects;
		}
		catch (JSONException e)
		{
			e.printStackTrace ();
			return null;
		}
	}

	public static JSONObject getJsonError (String errorMessage)
	{
		try
		{
			JSONObject jsonObj = new JSONObject ();
			jsonObj.put ("ERROR", errorMessage);
			return jsonObj;
		}
		catch (JSONException e)
		{
			e.printStackTrace ();
			return null;
		}
	}
	
	public static JSONObject getJsonMessage (String message)
	{
		try
		{
			JSONObject jsonObj = new JSONObject ();
			jsonObj.put ("MESSAGE", message);
			return jsonObj;
		}
		catch (JSONException e)
		{
			e.printStackTrace ();
			return null;
		}
	}
}


package org.openmrs.module.mobile.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil
{
	public static final String	FE_FORMAT		= "dd/MM/yyyy HH:mm:ss";
	public static final String	SQL_DATETIME	= "yyyy-MM-dd HH:mm:ss";
	public static final String	FE_FORMAT_TRUNC	= "dd/MM/yyyy";
	public static final String	DOB_FORMAT		= "dd/MM/yyyy";
	public static final String	SQL_DATE		= "yyyy-MM-dd";

	public static Date getDateFromString (String string, String format) throws ParseException
	{
		SimpleDateFormat sdf = new SimpleDateFormat (format);
		return sdf.parse (string);
	}

	public static String convertToSQL (String string, String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat (format);
		Date date1;
		try
		{
			date1 = sdf.parse (string);
		}
		catch (ParseException e)
		{
			e.printStackTrace ();
			return null;
		}

		sdf.applyPattern (SQL_DATETIME);
		return sdf.format (date1);
	}

	public static String getSQLDate (Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat (SQL_DATETIME);
		return sdf.format (date);
	}
}

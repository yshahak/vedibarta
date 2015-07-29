package org.vedibarta.app;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.splunk.mint.Mint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class Utilities {

	static final String FILE_NUMBERS = "numbers.txt";
	static final String FILE_PATHES = "pathes.txt";

	// Return the size of a directory in bytes
	public long dirSize(File dir) {
		long result = 0;
		Stack<File> dirlist = new Stack<File>();
		dirlist.clear();
		dirlist.push(dir);
		while (!dirlist.isEmpty()) {
			File dirCurrent = dirlist.pop();
			File[] fileList = dirCurrent.listFiles();
			for (File f : fileList) {
				if (f.isDirectory())
					dirlist.push(f);
				else
					result += f.length();
			}
		}
		return result;
	}

	/**
	 * Function to convert milliseconds time to Timer Format
	 * Hours:Minutes:Seconds
	 * */
	public String milliSecondsToTimer(long milliseconds) {
		String finalTimerString = "";
		String secondsString = "";

		// Convert total duration into time
		int hours = (int) (milliseconds / (1000 * 60 * 60));
		int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
		int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
		// Add hours if there
		if (hours > 0) {
			finalTimerString = hours + ":";
		}

		// Prepending 0 to seconds if it is one digit
		if (seconds < 10) {
			secondsString = "0" + seconds;
		} else {
			secondsString = "" + seconds;
		}

		finalTimerString = finalTimerString + minutes + ":" + secondsString;

		// return timer string
		return finalTimerString;
	}

	/**
	 * Function to get Progress percentage
	 * 
	 * @param currentDuration
	 * @param totalDuration
	 * */
	public int getProgressPercentage(long currentDuration, long totalDuration) {
		Double percentage = (double) 0;

		long currentSeconds = (int) (currentDuration / 1000);
		long totalSeconds = (int) (totalDuration / 1000);

		// calculating percentage
		percentage = (((double) currentSeconds) / totalSeconds) * 100;

		// return percentage
		return percentage.intValue();
	}

	/**
	 * Function to change progress to timer
	 * 
	 * @param progress
	 *            -
	 * @param totalDuration
	 *            returns current duration in milliseconds
	 * */
	public int progressToTimer(int progress, int totalDuration) {
		int currentDuration = 0;
		totalDuration = (int) (totalDuration / 1000);
		currentDuration = (int) ((((double) progress) / 100) * totalDuration);

		// return current duration in milliseconds
		return currentDuration * 1000;
	}

	public static boolean isNetworkAvailable(Context ctx) {
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	// Check vaildation of email adress
	public boolean isValidEmail(CharSequence target) {
		if (TextUtils.isEmpty(target)) {
			return false;
		} else {
			return android.util.Patterns.EMAIL_ADDRESS.matcher(target)
					.matches();
		}
	}

	public void writeToFile(String data, boolean numbers, Context ctx) {
		try {
			File TEXTFILES = new File(ctx.getFilesDir(), "TEXTFILES");
			if (!TEXTFILES.exists())
				TEXTFILES.mkdirs();
			FileWriter fileWriter;
			if (numbers)
				fileWriter = new FileWriter(new File(TEXTFILES, FILE_NUMBERS),
						true);
			else
				fileWriter = new FileWriter(new File(TEXTFILES, FILE_PATHES),
						true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(data);
			bufferedWriter.newLine();
			bufferedWriter.close();

		} catch (IOException e) {
            Mint.logException(e);
		}
	}

	public static  ArrayList<String> readFromFile(Context ctx, boolean numbers) {
		ArrayList<String> ret = new ArrayList<String>();
		File TEXTFILES = new File(ctx.getFilesDir(), "TEXTFILES");
		File file;
		if (numbers)
			file = new File(TEXTFILES, FILE_NUMBERS);
		else
			file = new File(TEXTFILES, FILE_PATHES);
		// file.delete();
		if (file.exists()) {
			try {
				FileReader inputStreamReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				String receiveString = "";
				// StringBuilder stringBuilder = new StringBuilder();
				while ((receiveString = bufferedReader.readLine()) != null) {
					ret.add(receiveString);
					// stringBuilder.append(receiveString);
				}
				bufferedReader.close();
			} catch (FileNotFoundException e) {
                Mint.logException(e);
			} catch (IOException e) {
                Mint.logException(e);
			}
		}
		return ret;
	}

	public void updateLine(Context ctx, int lineIndex, boolean replace,
			String replacement) throws IOException {
		File TEXTFILES = new File(ctx.getFilesDir(), "TEXTFILES");
		File[] files = { new File(TEXTFILES, FILE_NUMBERS),
				new File(TEXTFILES, FILE_PATHES) };
		if (replace) {
			final List<String> lines = new LinkedList<String>();
			final Scanner reader = new Scanner(new FileInputStream(files[1]),
					"UTF-8");
			while (reader.hasNextLine())
				lines.add(reader.nextLine());
			reader.close();
			assert lineIndex >= 0 && lineIndex <= lines.size() - 1;
			lines.set(lineIndex, replacement);
			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					files[1], false));
			for (final String line : lines) {
				writer.write(line);
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} else {
			for (int i = 0; i < 2; i++) {
				final List<String> lines = new LinkedList<String>();
				final Scanner reader = new Scanner(
						new FileInputStream(files[i]), "UTF-8");
				while (reader.hasNextLine())
					lines.add(reader.nextLine());
				reader.close();
				assert lineIndex >= 0 && lineIndex <= lines.size() - 1;
				lines.remove(lineIndex);
				final BufferedWriter writer = new BufferedWriter(
						new FileWriter(files[i], false));
				for (final String line : lines) {
					writer.write(line);
					writer.newLine();
				}
				writer.flush();
				writer.close();
			}
		}
	}
	
	//Check if service is running
	public static boolean isMyServiceRunning(Context ctx) {
	    ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (PlayingService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}

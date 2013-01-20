package de.ahans.scimark2;

import jnt.scimark2.Constants;
import jnt.scimark2.Random;
import jnt.scimark2.kernel;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity extends Activity
{	
	private TextView textView;
	private boolean running;
	private ScimarkThread thread;
	private int count;
	private ActivityIndicatorHandler activityIndicatorHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		textView = (TextView) findViewById(R.id.textview);
		textView.setText("Tab screen to run scimark.");
		running = false;
		count = 0;
		activityIndicatorHandler = new ActivityIndicatorHandler();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onPause() {
		Log.d(getClass().getName(), "onPause");
		if (thread != null) thread.cancel(true);
		super.onPause();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP && !running) {
			Log.d(getClass().getName(), "starting thread");
			thread = new ScimarkThread();
			thread.execute();
			running = true;
			count = 0;
			indicateActivity();
		}
		return super.onTouchEvent(event);
	}
	
	private void threadFinished(String out) {
		Log.d(getClass().getName(), "setting output");
		out += "\n\nTouch to rerun.";
		textView.setText(out);
		running = false;
		thread = null;
	}
	
	private void threadCancelled() {
		Log.d(getClass().getName(), "threadCancelled");
		textView.setText("thread cancelled");
		running = false;
		thread = null;
	}
	
	@SuppressLint("HandlerLeak")
	private class ActivityIndicatorHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			indicateActivity();
		}
		
		public void sleep(long delayMillis) {
			removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	}
	
	private void indicateActivity() {
		if (running) {
			StringBuilder sb = new StringBuilder("Running scimark2");
			for (int i = 0; i < count%4; i++) sb.append('.');
			textView.setText(sb.toString());
			count++;
			activityIndicatorHandler.sleep(1000);
		}
	}
	
	private class ScimarkThread extends AsyncTask<Void, Void, String>
	{
		private String result;

		@Override
		protected String doInBackground(Void... params) {
			Log.d(getClass().getName(), "doInBackground");
			runScimark();
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d(getClass().getName(), "onPostExecute");
			super.onPostExecute(result);
			threadFinished(result);
		}

		@Override
		protected void onCancelled() {
			Log.d(getClass().getName(), "onCancelled");
			threadCancelled();
			super.onCancelled();
		}

		private void runScimark() {
			// default to the (small) cache-contained version
			double min_time = Constants.RESOLUTION_DEFAULT;

			int FFT_size = Constants.FFT_SIZE;
			int SOR_size =  Constants.SOR_SIZE;
			int Sparse_size_M = Constants.SPARSE_SIZE_M;
			int Sparse_size_nz = Constants.SPARSE_SIZE_nz;
			int LU_size = Constants.LU_SIZE;

			// run the benchmark
			double res[] = new double[6];
			Random R = new Random(Constants.RANDOM_SEED);

			res[1] = kernel.measureFFT( FFT_size, min_time, R);
			res[2] = kernel.measureSOR( SOR_size, min_time, R);
			res[3] = kernel.measureMonteCarlo(min_time, R);
			res[4] = kernel.measureSparseMatmult( Sparse_size_M, 
						Sparse_size_nz, min_time, R);
			res[5] = kernel.measureLU( LU_size, min_time, R);
			res[0] = (res[1] + res[2] + res[3] + res[4] + res[5]) / 5.0;


		    // print out results
			
			StringBuilder sb = new StringBuilder();

			sb.append("SciMark 2.0a\n");
			sb.append("\n");
			sb.append("Composite Score: " + res[0] + "\n");
			sb.append("FFT ("+FFT_size+"): ");
			if (res[1]==0.0) sb.append("ERROR, INVALID NUMERICAL RESULT!\n");
			else             sb.append(res[1] + "\n");

			sb.append("SOR ("+SOR_size+"x"+ SOR_size+"): " + res[2] + "\n");
			sb.append("Monte Carlo: " + res[3] + "\n");
			sb.append("Sparse matmult (N="+ Sparse_size_M + ", nz=" + Sparse_size_nz + "): " + res[4] + "\n");
			sb.append("LU (" + LU_size + "x" + LU_size + "): ");
			if (res[5]==0.0) sb.append("ERROR, INVALID NUMERICAL RESULT!");
			else             sb.append(res[5] + "\n");

			// print out System info
			sb.append("\n");
			sb.append("java.vendor: "  + System.getProperty("java.vendor")  + "\n");
			sb.append("java.version: " + System.getProperty("java.version") + "\n");
			sb.append("os.arch: "      + System.getProperty("os.arch")      + "\n");
			sb.append("os.name: "      + System.getProperty("os.name")      + "\n");
			sb.append("os.version: "   + System.getProperty("os.version")   + "\n");
			
			result = sb.toString();
		}
	}
}

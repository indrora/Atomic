package indrora.atomic;

import indrora.atomic.activity.ServersActivity;
import indrora.atomic.model.Settings;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class FirstRunActivity extends Activity {

	
	private Resources resources;
	private class HelpTopic
	{
		public int AddedIn;
		public int TextResource;
		public int TitleReasource;
		public HelpTopic(int add, int title, int text)
		{
			this.AddedIn = add;
			this.TitleReasource = title;
			this.TextResource = text;
		}
	}
	
	private static int currentPage = 0;
	
	public HelpTopic[] topics = new HelpTopic[] {
			new HelpTopic(16, R.string.helptopic_introduction_title, R.string.helptopic_introduction_body),
			new HelpTopic(16, R.string.helptopic_conversation_title, R.string.helptopic_conversation_body),
			new HelpTopic(16, R.string.helptopic_colorschemes_title, R.string.helptopic_colorschemes_body),
			new HelpTopic(16, R.string.helptopic_notification_title, R.string.helptopic_notification_body)
	};
	
	private Html.ImageGetter assetImageGetter = new Html.ImageGetter() {
		
		@Override
		public Drawable getDrawable(String source) {

			Log.d("AssetImageGetter", "Get resource: "+source);
			try {
				InputStream sourceIS = resources.getAssets().open("help/"+source);
				Drawable sourceDrawable = Drawable.createFromStream(sourceIS, "");
				DisplayMetrics outMetrics = new DisplayMetrics();
				FirstRunActivity.this.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
				// Scale the image
				
				double scale = (float)(outMetrics.widthPixels) / (float)(sourceDrawable.getIntrinsicWidth());

				int width = sourceDrawable.getIntrinsicWidth();
				int height = sourceDrawable.getIntrinsicHeight();
				if(outMetrics.widthPixels < width)
				{
					width = outMetrics.widthPixels;
					height = (int) (sourceDrawable.getIntrinsicHeight() * scale);
				}
				Log.d("AssetImageGetter", String.format("New image dimentions: (%f scale) <%d x %d>", scale, width, height));
				sourceDrawable.setBounds(0, 0, (int)width, (int)height);
				
				return sourceDrawable;
				
			} catch (IOException e)  {
				return getResources().getDrawable(R.drawable.error);
			}
		}
	};
	
	private Settings ss;
	private ViewFlipper vf;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.firstrun);
		
		resources = this.getResources();
		
		// We're going to create a bunch of views to smash into the wizard. 
		// These are really going to be TextViews, but with Html loaded into them.
		
		vf = (ViewFlipper)findViewById(R.id.wizard_flipper);
		
		View v = View.inflate(this, R.layout.firstrunintro, null);
		try {
			((TextView)v.findViewById(R.id.versionlabel)).setText(String.format("Version %1$s",
					getPackageManager().getPackageInfo(getPackageName(), 0).versionName
					));
		} catch (NameNotFoundException e) {
			((TextView)v.findViewById(R.id.versionlabel)).setText("Unkown version!");
		}
		TextView firstView = ((TextView)v.findViewById(R.id.blathertext));
		firstView.setText(Html.fromHtml(getString(R.string.helptopic_titlepage_body)));
		firstView.setLinksClickable(true);
		firstView.setMovementMethod(LinkMovementMethod.getInstance());
		v.setTag(getString(R.string.helptopic_titlepage_title));
		
		vf.addView(wrapScrollview(v));
		vf.setScrollContainer(true);
		ss = new Settings(this);
		for(int i = ss.getLastRunVersion(); i <= ss.getCurrentVersion(); i++ )
		{
			for(HelpTopic ht : topics)
			{
				if(ht.AddedIn == i)
				{
					
					vf.addView(generateHelpPage(ht));
				}
			}
		}
		Button nButton = (Button)findViewById(R.id.action_next);
		nButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!isLast())
				{
					vf.showNext();
				}
				else
				{
					// We're all cool, be done.
					FirstRunActivity.this.finish();
				}
				updateButton();
				updateTitle();
			}
		});
		Button pButton = (Button)findViewById(R.id.action_prev);
		pButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!isFirst())
				{
					vf.showPrevious();
				}
				updateButton();
				updateTitle();
			}
		});
		if(currentPage>0)
		{
			vf.setDisplayedChild(currentPage);
		}
		updateButton();
		updateTitle();
		vf.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		vf.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		
	}
	
	@Override
	protected void onPause() {
		currentPage = vf.getDisplayedChild();
		super.onPause();
	}
	
	protected void updateButton()
	{
		Button pb = (Button)findViewById(R.id.action_prev);
		pb.setVisibility(isFirst()?Button.GONE:Button.VISIBLE);
	}
	
	protected boolean isFirst() {
		return vf.getDisplayedChild() == 0;
	}
	protected boolean isLast()
	{
		return vf.getDisplayedChild() == vf.getChildCount()-1;
	}
	
	protected void updateTitle()
	{
		this.setTitle((CharSequence)(vf.getCurrentView().getTag()));
	}
	
	@Override
	public void onBackPressed() {
		final FirstRunActivity t = this;
		AlertDialog ab = new AlertDialog.Builder(this)
			.setTitle(R.string.title_activity_first_run)
			.setMessage(R.string.back_twice_exit)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					cleanup();
				}
			})
			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			})
			.create();
		ab.show();
		}
	protected void cleanup() {
		Intent i = new Intent(this,ServersActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
	}
	protected View wrapScrollview(View vv)
	{
		ScrollView scrollable = new ScrollView(this);
		scrollable.addView(vv);
		return scrollable;
	}
	protected View generateHelpPage(HelpTopic topic) {
		TextView contentView = new TextView(this);
		Spanned st = Html.fromHtml(getString(topic.TextResource), assetImageGetter, null);
		contentView.setText(st);
		contentView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		contentView.setGravity(Gravity.CENTER_HORIZONTAL);
		
		View sv = wrapScrollview(contentView);
		sv.setTag(getString(topic.TitleReasource));
		return sv;
	}
}

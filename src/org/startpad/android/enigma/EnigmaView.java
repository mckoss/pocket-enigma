package org.startpad.android.enigma;

import org.startpad.Enigma;
import org.startpad.android.enigma.EnigmaApp.SoundEffect;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class EnigmaView extends View {
    private Context context;
	private Resources res;
	
	Enigma machine;
	Drawable letters;
	
	private static final String TAG = "EnigmaView";
	private static int simWidth = 1024;
	private static int simHeight = 1200;
	private int viewWidth;
	private int viewHeight;
	
    private float xScale;
    private float yScale;
	
	private Rect[] rcRotors = new Rect[3];
	private Rect rcAllRotors;
	private Rect[] rcSpinners = new Rect[3];
	private Rect rcLetters;
	private Rect rcScrews[] = new Rect[2];
	private int[] adySpin = new int[3];
	private int dyLetterRotor;
	private static float SPIN_SENSITIVITY = (float) 0.5;
	
	QWERTZU qLights = new QWERTZU();
	QWERTZU qKeys = new QWERTZU();
	
	boolean fDown = false;
	Point ptClickLast;
	long msDown;
	char chLight = 0;
	Handler handler = new Handler();
	
	boolean fLidClosed = true;
	boolean fCoverOpen = false;
	int iSpinning = -1;
	int diSpinLast;
	
	Toast toast;
	
	// Registration points for known positions on the engima image
    enum RegPoint
       {
       DPT_ROTOR(R.dimen.dx_rotor, R.dimen.dy_rotor),
       DPT_SPINNER(R.dimen.dx_spinner, R.dimen.dy_spinner),
       
       PT_LETTERS(R.dimen.x_letters, R.dimen.y_letters),
       DPT_LETTERS(R.dimen.dx_letters, R.dimen.dy_letters),
       
       PT_P_KEY(R.dimen.x_P_key, R.dimen.y_P_key),
       DPT_KEY(R.dimen.dx_key, R.dimen.dy_key),
       DPT_RIGHT_KEY(R.dimen.dx_right_key, 0),
       DPT_UP_KEY(R.dimen.dx_up_key, R.dimen.dy_up_key),
       
       PT_P_LIGHT(R.dimen.x_P_light, R.dimen.y_P_light),
       DPT_LIGHT(R.dimen.dx_light, R.dimen.dy_light),
       DPT_RIGHT_LIGHT(R.dimen.dx_right_light, 0),
       DPT_UP_LIGHT(R.dimen.dx_up_light, R.dimen.dy_up_light),
       
       PT_LEFT_SCREW(R.dimen.x_left_screw, R.dimen.y_screw),
       PT_RIGHT_SCREW(R.dimen.x_right_screw, R.dimen.y_screw),
       DPT_SCREW(R.dimen.dx_screw, R.dimen.dy_screw),
       ;

       private int ridX;
       private int ridY;
       private Point ptRaw;
       private Point ptScaled;
       
       RegPoint(int ridX, int ridY)
           {
           this.ridX = ridX;
           this.ridY = ridY;
           }
       
       public void init(Resources res, float xScale, float yScale)
           {
           ensurePoint(res);
           ptScaled = new Point((int) (ptRaw.x * xScale), (int) (ptRaw.y * yScale));
           }
       
       private void ensurePoint(Resources res)
           {
           if (ptRaw == null)
               {
               int x = 0;
               int y = 0;
               
               if (ridX != 0)
                   x = (int) (res.getDimension(ridX));
               if (ridY != 0)
                   y = (int) (res.getDimension(ridY));
               ptRaw = new Point(x,y);
               }
           }
       
       public Point getPoint()
           {
           return ptScaled;
           }
       
       static Rect rectFromPtDpt(RegPoint rpt, RegPoint rdpt)
           {
           Point pt = rpt.getPoint();
           Point dpt = rdpt.getPoint();
           return new Rect(pt.x, pt.y, pt.x + dpt.x, pt.y + dpt.y);
           }
       }
	
	protected void onMeasure(int xSpec, int ySpec)
		{
		super.onMeasure(xSpec, ySpec);
		viewWidth = getMeasuredWidth();
		viewHeight = getMeasuredHeight();
		int yRotor;
		int[] axRotors = new int[3];
		
		xScale = (float) viewWidth/simWidth;
		yScale = (float) viewHeight/simHeight;
		
		Log.d(TAG, "Scaling: " + xScale + ", " + yScale);
		
        for (RegPoint rpt : RegPoint.values())
            rpt.init(this.res, xScale, yScale);
		
		// Setup rotor windows and spinner rects - from centers and window sizes
		yRotor = (int) (res.getDimension(R.dimen.y_rotors) * yScale);
		axRotors[0] = (int) (res.getDimension(R.dimen.x_left_rotor) * xScale);
		axRotors[1] = (int) (res.getDimension(R.dimen.x_center_rotor) * xScale);
		axRotors[2] = (int) (res.getDimension(R.dimen.x_right_rotor) * xScale);
		
		Point dptRotor = RegPoint.DPT_ROTOR.getPoint();
		Point dptSpinner = RegPoint.DPT_SPINNER.getPoint();
		for (int i = 0; i < 3; i++)
			{
			rcRotors[i] = new Rect(axRotors[i]-dptRotor.x/2, yRotor-dptRotor.y/2,
								   axRotors[i]+dptRotor.x/2, yRotor+dptRotor.y/2);
            if (i == 0)
                rcAllRotors = new Rect(rcRotors[i]);
            else
                rcAllRotors.union(rcRotors[i]);
            
            // Adjust for baselilne of a character
			rcRotors[i].inset((int) (xScale*7), (int) (yScale*20));
			rcRotors[i].offset(0, -1);
			
			rcSpinners[i] = new Rect(rcRotors[i].centerX(), rcRotors[i].centerY() - dptSpinner.y/2,
			                         rcRotors[i].centerX() + dptSpinner.x, rcRotors[i].centerY() + dptSpinner.y/2);
			}
		dyLetterRotor = rcRotors[0].height();
		
		// Setup lights overlay PNG
		rcLetters = RegPoint.rectFromPtDpt(RegPoint.PT_LETTERS, RegPoint.DPT_LETTERS);
		
		// Setup dimensions of keyboard for hit testing
		Rect rcPKey = RegPoint.rectFromPtDpt(RegPoint.PT_P_KEY, RegPoint.DPT_KEY);
		qKeys.setSize(rcPKey, RegPoint.DPT_RIGHT_KEY.getPoint(), RegPoint.DPT_UP_KEY.getPoint());
		
		// Setup dimensions of lights for display
		Rect rcPLight = RegPoint.rectFromPtDpt(RegPoint.PT_P_LIGHT, RegPoint.DPT_LIGHT);
		qLights.setSize(rcPLight, RegPoint.DPT_RIGHT_LIGHT.getPoint(), RegPoint.DPT_UP_LIGHT.getPoint());
		
		rcScrews[0] = RegPoint.rectFromPtDpt(RegPoint.PT_LEFT_SCREW, RegPoint.DPT_SCREW);
		rcScrews[1] = RegPoint.rectFromPtDpt(RegPoint.PT_RIGHT_SCREW, RegPoint.DPT_SCREW);
		for (int i = 0; i < 2; i++)
		    rcScrews[i].inset(-5, -5);
		}
	
	protected void onDraw(Canvas canvas)
		{
		super.onDraw(canvas);
		if (fLidClosed || fCoverOpen)
			return;
		
		Paint paint = new Paint();
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setColor(Color.BLACK);
		paint.setTextSize(rcRotors[0].height());
		paint.setAntiAlias(true);
		
		for (int i = 0; i < 3; i++)
			{
			// Draw letter above and below the current letter (assume up to 3 letters partially visible
			// in the rotor window.
			int iCenter = machine.position[i];
			int dy = (int) (Math.abs(adySpin[i]) * SPIN_SENSITIVITY);
			int diLetter = dy / dyLetterRotor;
			int dyNudge = dy % dyLetterRotor;
			if (adySpin[i] < 0)
			    {
			    iCenter = (iCenter + diLetter) % 26;
			    dyNudge = -dyNudge;
			    }
			else
			    {
			    iCenter = (iCenter + (26 - diLetter)) % 26;
			    }
			
			canvas.save();
			canvas.clipRect(rcAllRotors);
			for (int j = -1; j <= 1; j++)
			    {
			    String sChar = Character.toString(Enigma.chFromI((iCenter + j + 26) % 26));
			    canvas.drawText(sChar, rcRotors[i].centerX(),
			                    rcRotors[i].bottom + dyNudge + j * dyLetterRotor,
			                    paint);
			    }
			canvas.restore();
			}
		
		if (fDown)
		    {
		    canvas.save();
		    canvas.clipRect(qLights.rectFromChar(chLight));
    		letters.setBounds(rcLetters);
    		letters.draw(canvas);
    		canvas.restore();
		    }
		}
	
	protected int diRotorSpin(int dy)
	    {
	    int dyT = (int) (Math.abs(dy) * SPIN_SENSITIVITY);
	    int di = (dyT + dyLetterRotor/2) / dyLetterRotor;
	    if (dy < 0)
	        return di % 26;
	    else
	        return (26 - di) % 26;
	    }

	public EnigmaView(Context context) {
		super(context);
		init(context);
	}

	public EnigmaView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public EnigmaView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	public void setMachine(Enigma machine)
		{
		this.machine = machine;	
		}
	
    // Initialize Simulation View
    private void init(Context context)
	    {
	    this.context = context;
    	this.res = context.getResources();
    	this.letters = this.res.getDrawable(R.drawable.letters);
    	
        toast = Toast.makeText(context, R.string.startup_message, Toast.LENGTH_LONG);
        toast.show();
    	
	    setOnTouchListener(new OnTouchListener()
	        {
	        public boolean onTouch(View view, MotionEvent event)
	            {
	            switch (event.getAction())
	            {
	            case MotionEvent.ACTION_DOWN:
	                // Defensive?
	                if (fDown || iSpinning >= 0)
	                    return false;
	                
                    if (fLidClosed)
                        {
                        fLidClosed = false;
                        setBackgroundResource(R.drawable.enigma);
                        invalidate();
                        
                        // BUG: Why is this EnigmaView cast needed here - but other
                        // class variable refs are not?
                        toast = Toast.makeText(((EnigmaView) view).context, R.string.sim_hint, Toast.LENGTH_LONG);
                        toast.show();
                        return true;
                        }
                    
                    if (fCoverOpen)
                        {
                        fCoverOpen = false;
                        setBackgroundResource(R.drawable.enigma);
                        invalidate();
                        return true;
                        }
	            
                    Point ptClick = new Point((int) event.getX(), (int) event.getY());
	                
                    // Open Lid, if user clicks on the lid screws
	                for (int i = 0; i < 2; i++)
	                    if (rcScrews[i].contains(ptClick.x, ptClick.y))
	                        {
	                        fCoverOpen = true;
	                        setBackgroundResource(R.drawable.cover_open);
	                        invalidate();
	                        return true;
	                        }
	                
	                // Press a key if user clicks the keyboard
	                char ch = qKeys.charFromPt(ptClick);
	                if (ch != 0)
	                    {
	                    fDown = true;
	                    msDown = System.currentTimeMillis();

	                    chLight = machine.encodeChar(ch);
	                    Log.d(TAG, "Encode " + ch + " -> " + chLight);
	                    
	                    invalidate(rcAllRotors);
	                    invalidate(qLights.rectFromChar(chLight));
	                        
	                    EnigmaApp.SoundEffect.KEY_DOWN.play();
	                    return true;
                        }

                    // Spin the rotors
                    for (int i = 0; i < 3; i++)
                        {
                        if (rcSpinners[i].contains(ptClick.x, ptClick.y))
                            {
                            iSpinning = i;
                            ptClickLast = new Point(ptClick);
                            diSpinLast = 0;
                            return true;
                            }
                        }
                    
                    break;
	                
	            case MotionEvent.ACTION_MOVE:
	                // TODO: Track user slides across keys
	                if (fDown)
	                    return false;
	                
	                Point ptMove = new Point((int) event.getX(), (int) event.getY());
	                
	                if (iSpinning != -1)
	                    {
	                    int dy = ptMove.y - ptClickLast.y;
	                    
	                    adySpin[iSpinning] = dy;
	                    invalidate(rcAllRotors);
	                    
                        int diSpin = diRotorSpin(dy);
                        if (diSpinLast != diSpin)
                            {
                            diSpinLast = diSpin;
                            EnigmaApp.SoundEffect.ROTOR.play();
                            }
	                    return true;
	                    }

	            case MotionEvent.ACTION_UP:
	                if (iSpinning != -1)
	                    {
	                    adySpin[iSpinning] = 0;
	                    machine.position[iSpinning] = (machine.position[iSpinning] + diSpinLast) % 26;
                        iSpinning = -1;
	                    invalidate(rcAllRotors);
	                    return true;
	                    }

	                if (fDown)
	                    {
	                    long msTime = System.currentTimeMillis() - msDown;
	                    if (msTime < 1000)
	                        {
	                        handler.postDelayed(new Runnable()
    	                        {
                                public void run()
                                    {
                                    doKeyUp();
                                    }
    	                        }, 1000 - msTime);
	                        return true;
	                        }
	                    doKeyUp();
	                    }
	                break;
	            }
	            return false;
	            }
	        });
	    }
    
    private void doKeyUp()
        {
        fDown = false;
        invalidate(qLights.rectFromChar(chLight));
        EnigmaApp.SoundEffect.KEY_UP.play();
        }
    
    /* Convert from x,y to a keyboard/light character.  Assumes QWERTU
     * character layout.
     */
    
    private static String[] asRows = new String[] {"QWERTZUIO", "ASDFGHJK", "PYXCVBNML"};
    
    class QWERTZU
        {
        private Point ptRight;
        private Rect[] arcRows;
        
        public void setSize(Rect rcP, Point ptRight, Point ptUp)
            {
            this.ptRight = new Point(ptRight);
            
            rcP = new Rect(rcP);
            
            Rect rcA = new Rect(rcP);
            rcA.offset(ptRight.x, ptRight.y);
            rcA.offset(ptUp.x, ptUp.y);
            
            Rect rcQ = new Rect(rcA);
            rcQ.offset(ptUp.x, ptUp.y);
            
            arcRows = new Rect[] {rcQ, rcA, rcP};
            }
        
        public char charFromPt(Point ptClick)
            {
            int d2Min = -1;
            char chBest = 0;
            
            if (ptClick.y < arcRows[0].top - 10)
                return 0;
            
            for (int i = 0; i < arcRows.length; i++)
                {
                Rect rcChar = new Rect(arcRows[i]);

                for (int j = 0; j < asRows[i].length(); j++)
                    {
                    Point ptChar = new Point(rcChar.centerX(), rcChar.centerY());
                    int d2 = (int) (Math.pow(ptChar.x - ptClick.x,2) + Math.pow(ptChar.y - ptClick.y, 2));
                    if (d2Min < 0 || d2 < d2Min)
                        {
                        d2Min = d2;
                        chBest = asRows[i].charAt(j);
                        }
                    rcChar.offset(ptRight.x, ptRight.y);
                    }
                }
            
            return chBest;
            }
        
        public Rect rectFromChar(char ch)
            {
            for (int i = 0; i < arcRows.length; i++)
                {
                Rect rcChar = new Rect(arcRows[i]);

                for (int j = 0; j < asRows[i].length(); j++)
                    {
                    if (ch == asRows[i].charAt(j))
                        return rcChar;
                    rcChar.offset(ptRight.x, ptRight.y);
                    }
                }
            
            return null;
            }
        }
}

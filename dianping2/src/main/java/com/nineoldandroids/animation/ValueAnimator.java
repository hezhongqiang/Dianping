package com.nineoldandroids.animation;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AndroidRuntimeException;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ValueAnimator extends Animator
{
  static final int ANIMATION_FRAME = 1;
  static final int ANIMATION_START = 0;
  private static final long DEFAULT_FRAME_DELAY = 10L;
  public static final int INFINITE = -1;
  public static final int RESTART = 1;
  public static final int REVERSE = 2;
  static final int RUNNING = 1;
  static final int SEEKED = 2;
  static final int STOPPED = 0;
  private static ThreadLocal<AnimationHandler> sAnimationHandler = new ThreadLocal();
  private static final ThreadLocal<ArrayList<ValueAnimator>> sAnimations = new ThreadLocal()
  {
    protected ArrayList<ValueAnimator> initialValue()
    {
      return new ArrayList();
    }
  };
  private static final Interpolator sDefaultInterpolator;
  private static final ThreadLocal<ArrayList<ValueAnimator>> sDelayedAnims;
  private static final ThreadLocal<ArrayList<ValueAnimator>> sEndingAnims;
  private static final TypeEvaluator sFloatEvaluator;
  private static long sFrameDelay;
  private static final TypeEvaluator sIntEvaluator;
  private static final ThreadLocal<ArrayList<ValueAnimator>> sPendingAnimations = new ThreadLocal()
  {
    protected ArrayList<ValueAnimator> initialValue()
    {
      return new ArrayList();
    }
  };
  private static final ThreadLocal<ArrayList<ValueAnimator>> sReadyAnims;
  private float mCurrentFraction = 0.0F;
  private int mCurrentIteration = 0;
  private long mDelayStartTime;
  private long mDuration = 300L;
  boolean mInitialized = false;
  private Interpolator mInterpolator = sDefaultInterpolator;
  private boolean mPlayingBackwards = false;
  int mPlayingState = 0;
  private int mRepeatCount = 0;
  private int mRepeatMode = 1;
  private boolean mRunning = false;
  long mSeekTime = -1L;
  private long mStartDelay = 0L;
  long mStartTime;
  private boolean mStarted = false;
  private boolean mStartedDelay = false;
  private ArrayList<AnimatorUpdateListener> mUpdateListeners = null;
  PropertyValuesHolder[] mValues;
  HashMap<String, PropertyValuesHolder> mValuesMap;

  static
  {
    sDelayedAnims = new ThreadLocal()
    {
      protected ArrayList<ValueAnimator> initialValue()
      {
        return new ArrayList();
      }
    };
    sEndingAnims = new ThreadLocal()
    {
      protected ArrayList<ValueAnimator> initialValue()
      {
        return new ArrayList();
      }
    };
    sReadyAnims = new ThreadLocal()
    {
      protected ArrayList<ValueAnimator> initialValue()
      {
        return new ArrayList();
      }
    };
    sDefaultInterpolator = new AccelerateDecelerateInterpolator();
    sIntEvaluator = new IntEvaluator();
    sFloatEvaluator = new FloatEvaluator();
    sFrameDelay = 10L;
  }

  public static void clearAllAnimations()
  {
    ((ArrayList)sAnimations.get()).clear();
    ((ArrayList)sPendingAnimations.get()).clear();
    ((ArrayList)sDelayedAnims.get()).clear();
  }

  private boolean delayedAnimationFrame(long paramLong)
  {
    if (!this.mStartedDelay)
    {
      this.mStartedDelay = true;
      this.mDelayStartTime = paramLong;
    }
    long l;
    do
    {
      return false;
      l = paramLong - this.mDelayStartTime;
    }
    while (l <= this.mStartDelay);
    this.mStartTime = (paramLong - (l - this.mStartDelay));
    this.mPlayingState = 1;
    return true;
  }

  private void endAnimation()
  {
    ((ArrayList)sAnimations.get()).remove(this);
    ((ArrayList)sPendingAnimations.get()).remove(this);
    ((ArrayList)sDelayedAnims.get()).remove(this);
    this.mPlayingState = 0;
    if ((this.mRunning) && (this.mListeners != null))
    {
      ArrayList localArrayList = (ArrayList)this.mListeners.clone();
      int j = localArrayList.size();
      int i = 0;
      while (i < j)
      {
        ((Animator.AnimatorListener)localArrayList.get(i)).onAnimationEnd(this);
        i += 1;
      }
    }
    this.mRunning = false;
    this.mStarted = false;
  }

  public static int getCurrentAnimationsCount()
  {
    return ((ArrayList)sAnimations.get()).size();
  }

  public static long getFrameDelay()
  {
    return sFrameDelay;
  }

  public static ValueAnimator ofFloat(float[] paramArrayOfFloat)
  {
    ValueAnimator localValueAnimator = new ValueAnimator();
    localValueAnimator.setFloatValues(paramArrayOfFloat);
    return localValueAnimator;
  }

  public static ValueAnimator ofInt(int[] paramArrayOfInt)
  {
    ValueAnimator localValueAnimator = new ValueAnimator();
    localValueAnimator.setIntValues(paramArrayOfInt);
    return localValueAnimator;
  }

  public static ValueAnimator ofObject(TypeEvaluator paramTypeEvaluator, Object[] paramArrayOfObject)
  {
    ValueAnimator localValueAnimator = new ValueAnimator();
    localValueAnimator.setObjectValues(paramArrayOfObject);
    localValueAnimator.setEvaluator(paramTypeEvaluator);
    return localValueAnimator;
  }

  public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder[] paramArrayOfPropertyValuesHolder)
  {
    ValueAnimator localValueAnimator = new ValueAnimator();
    localValueAnimator.setValues(paramArrayOfPropertyValuesHolder);
    return localValueAnimator;
  }

  public static void setFrameDelay(long paramLong)
  {
    sFrameDelay = paramLong;
  }

  private void start(boolean paramBoolean)
  {
    if (Looper.myLooper() == null)
      throw new AndroidRuntimeException("Animators may only be run on Looper threads");
    this.mPlayingBackwards = paramBoolean;
    this.mCurrentIteration = 0;
    this.mPlayingState = 0;
    this.mStarted = true;
    this.mStartedDelay = false;
    ((ArrayList)sPendingAnimations.get()).add(this);
    if (this.mStartDelay == 0L)
    {
      setCurrentPlayTime(getCurrentPlayTime());
      this.mPlayingState = 0;
      this.mRunning = true;
      if (this.mListeners != null)
      {
        localObject = (ArrayList)this.mListeners.clone();
        int j = ((ArrayList)localObject).size();
        int i = 0;
        while (i < j)
        {
          ((Animator.AnimatorListener)((ArrayList)localObject).get(i)).onAnimationStart(this);
          i += 1;
        }
      }
    }
    AnimationHandler localAnimationHandler = (AnimationHandler)sAnimationHandler.get();
    Object localObject = localAnimationHandler;
    if (localAnimationHandler == null)
    {
      localObject = new AnimationHandler(null);
      sAnimationHandler.set(localObject);
    }
    ((AnimationHandler)localObject).sendEmptyMessage(0);
  }

  private void startAnimation()
  {
    initAnimation();
    ((ArrayList)sAnimations.get()).add(this);
    if ((this.mStartDelay > 0L) && (this.mListeners != null))
    {
      ArrayList localArrayList = (ArrayList)this.mListeners.clone();
      int j = localArrayList.size();
      int i = 0;
      while (i < j)
      {
        ((Animator.AnimatorListener)localArrayList.get(i)).onAnimationStart(this);
        i += 1;
      }
    }
  }

  public void addUpdateListener(AnimatorUpdateListener paramAnimatorUpdateListener)
  {
    if (this.mUpdateListeners == null)
      this.mUpdateListeners = new ArrayList();
    this.mUpdateListeners.add(paramAnimatorUpdateListener);
  }

  void animateValue(float paramFloat)
  {
    paramFloat = this.mInterpolator.getInterpolation(paramFloat);
    this.mCurrentFraction = paramFloat;
    int j = this.mValues.length;
    int i = 0;
    while (i < j)
    {
      this.mValues[i].calculateValue(paramFloat);
      i += 1;
    }
    if (this.mUpdateListeners != null)
    {
      j = this.mUpdateListeners.size();
      i = 0;
      while (i < j)
      {
        ((AnimatorUpdateListener)this.mUpdateListeners.get(i)).onAnimationUpdate(this);
        i += 1;
      }
    }
  }

  boolean animationFrame(long paramLong)
  {
    boolean bool2 = false;
    if (this.mPlayingState == 0)
    {
      this.mPlayingState = 1;
      if (this.mSeekTime >= 0L)
        break label58;
      this.mStartTime = paramLong;
    }
    while (true)
      switch (this.mPlayingState)
      {
      default:
        return false;
        label58: this.mStartTime = (paramLong - this.mSeekTime);
        this.mSeekTime = -1L;
      case 1:
      case 2:
      }
    float f2;
    if (this.mDuration > 0L)
      f2 = (float)(paramLong - this.mStartTime) / (float)this.mDuration;
    boolean bool1;
    float f1;
    while (true)
    {
      bool1 = bool2;
      f1 = f2;
      if (f2 < 1.0F)
        break;
      if ((this.mCurrentIteration >= this.mRepeatCount) && (this.mRepeatCount != -1))
        break label282;
      if (this.mListeners != null)
      {
        int j = this.mListeners.size();
        int i = 0;
        while (true)
          if (i < j)
          {
            ((Animator.AnimatorListener)this.mListeners.get(i)).onAnimationRepeat(this);
            i += 1;
            continue;
            f2 = 1.0F;
            break;
          }
      }
      if (this.mRepeatMode == 2)
      {
        if (!this.mPlayingBackwards)
          break label276;
        bool1 = false;
        this.mPlayingBackwards = bool1;
      }
      this.mCurrentIteration += (int)f2;
      f1 = f2 % 1.0F;
      this.mStartTime += this.mDuration;
      bool1 = bool2;
    }
    while (true)
    {
      f2 = f1;
      if (this.mPlayingBackwards)
        f2 = 1.0F - f1;
      animateValue(f2);
      return bool1;
      label276: bool1 = true;
      break;
      label282: bool1 = true;
      f1 = Math.min(f2, 1.0F);
    }
  }

  public void cancel()
  {
    if ((this.mPlayingState != 0) || (((ArrayList)sPendingAnimations.get()).contains(this)) || (((ArrayList)sDelayedAnims.get()).contains(this)))
    {
      if ((this.mRunning) && (this.mListeners != null))
      {
        Iterator localIterator = ((ArrayList)this.mListeners.clone()).iterator();
        while (localIterator.hasNext())
          ((Animator.AnimatorListener)localIterator.next()).onAnimationCancel(this);
      }
      endAnimation();
    }
  }

  public ValueAnimator clone()
  {
    ValueAnimator localValueAnimator = (ValueAnimator)super.clone();
    int j;
    int i;
    if (this.mUpdateListeners != null)
    {
      localObject = this.mUpdateListeners;
      localValueAnimator.mUpdateListeners = new ArrayList();
      j = ((ArrayList)localObject).size();
      i = 0;
      while (i < j)
      {
        localValueAnimator.mUpdateListeners.add(((ArrayList)localObject).get(i));
        i += 1;
      }
    }
    localValueAnimator.mSeekTime = -1L;
    localValueAnimator.mPlayingBackwards = false;
    localValueAnimator.mCurrentIteration = 0;
    localValueAnimator.mInitialized = false;
    localValueAnimator.mPlayingState = 0;
    localValueAnimator.mStartedDelay = false;
    Object localObject = this.mValues;
    if (localObject != null)
    {
      j = localObject.length;
      localValueAnimator.mValues = new PropertyValuesHolder[j];
      localValueAnimator.mValuesMap = new HashMap(j);
      i = 0;
      while (i < j)
      {
        PropertyValuesHolder localPropertyValuesHolder = localObject[i].clone();
        localValueAnimator.mValues[i] = localPropertyValuesHolder;
        localValueAnimator.mValuesMap.put(localPropertyValuesHolder.getPropertyName(), localPropertyValuesHolder);
        i += 1;
      }
    }
    return (ValueAnimator)localValueAnimator;
  }

  public void end()
  {
    if ((!((ArrayList)sAnimations.get()).contains(this)) && (!((ArrayList)sPendingAnimations.get()).contains(this)))
    {
      this.mStartedDelay = false;
      startAnimation();
      if ((this.mRepeatCount <= 0) || ((this.mRepeatCount & 0x1) != 1))
        break label82;
      animateValue(0.0F);
    }
    while (true)
    {
      endAnimation();
      return;
      if (this.mInitialized)
        break;
      initAnimation();
      break;
      label82: animateValue(1.0F);
    }
  }

  public float getAnimatedFraction()
  {
    return this.mCurrentFraction;
  }

  public Object getAnimatedValue()
  {
    if ((this.mValues != null) && (this.mValues.length > 0))
      return this.mValues[0].getAnimatedValue();
    return null;
  }

  public Object getAnimatedValue(String paramString)
  {
    paramString = (PropertyValuesHolder)this.mValuesMap.get(paramString);
    if (paramString != null)
      return paramString.getAnimatedValue();
    return null;
  }

  public long getCurrentPlayTime()
  {
    if ((!this.mInitialized) || (this.mPlayingState == 0))
      return 0L;
    return AnimationUtils.currentAnimationTimeMillis() - this.mStartTime;
  }

  public long getDuration()
  {
    return this.mDuration;
  }

  public Interpolator getInterpolator()
  {
    return this.mInterpolator;
  }

  public int getRepeatCount()
  {
    return this.mRepeatCount;
  }

  public int getRepeatMode()
  {
    return this.mRepeatMode;
  }

  public long getStartDelay()
  {
    return this.mStartDelay;
  }

  public PropertyValuesHolder[] getValues()
  {
    return this.mValues;
  }

  void initAnimation()
  {
    if (!this.mInitialized)
    {
      int j = this.mValues.length;
      int i = 0;
      while (i < j)
      {
        this.mValues[i].init();
        i += 1;
      }
      this.mInitialized = true;
    }
  }

  public boolean isRunning()
  {
    return (this.mPlayingState == 1) || (this.mRunning);
  }

  public boolean isStarted()
  {
    return this.mStarted;
  }

  public void removeAllUpdateListeners()
  {
    if (this.mUpdateListeners == null)
      return;
    this.mUpdateListeners.clear();
    this.mUpdateListeners = null;
  }

  public void removeUpdateListener(AnimatorUpdateListener paramAnimatorUpdateListener)
  {
    if (this.mUpdateListeners == null);
    do
    {
      return;
      this.mUpdateListeners.remove(paramAnimatorUpdateListener);
    }
    while (this.mUpdateListeners.size() != 0);
    this.mUpdateListeners = null;
  }

  public void reverse()
  {
    if (!this.mPlayingBackwards);
    for (boolean bool = true; ; bool = false)
    {
      this.mPlayingBackwards = bool;
      if (this.mPlayingState != 1)
        break;
      long l1 = AnimationUtils.currentAnimationTimeMillis();
      long l2 = this.mStartTime;
      this.mStartTime = (l1 - (this.mDuration - (l1 - l2)));
      return;
    }
    start(true);
  }

  public void setCurrentPlayTime(long paramLong)
  {
    initAnimation();
    long l = AnimationUtils.currentAnimationTimeMillis();
    if (this.mPlayingState != 1)
    {
      this.mSeekTime = paramLong;
      this.mPlayingState = 2;
    }
    this.mStartTime = (l - paramLong);
    animationFrame(l);
  }

  public ValueAnimator setDuration(long paramLong)
  {
    if (paramLong < 0L)
      throw new IllegalArgumentException("Animators cannot have negative duration: " + paramLong);
    this.mDuration = paramLong;
    return this;
  }

  public void setEvaluator(TypeEvaluator paramTypeEvaluator)
  {
    if ((paramTypeEvaluator != null) && (this.mValues != null) && (this.mValues.length > 0))
      this.mValues[0].setEvaluator(paramTypeEvaluator);
  }

  public void setFloatValues(float[] paramArrayOfFloat)
  {
    if ((paramArrayOfFloat == null) || (paramArrayOfFloat.length == 0))
      return;
    if ((this.mValues == null) || (this.mValues.length == 0))
      setValues(new PropertyValuesHolder[] { PropertyValuesHolder.ofFloat("", paramArrayOfFloat) });
    while (true)
    {
      this.mInitialized = false;
      return;
      this.mValues[0].setFloatValues(paramArrayOfFloat);
    }
  }

  public void setIntValues(int[] paramArrayOfInt)
  {
    if ((paramArrayOfInt == null) || (paramArrayOfInt.length == 0))
      return;
    if ((this.mValues == null) || (this.mValues.length == 0))
      setValues(new PropertyValuesHolder[] { PropertyValuesHolder.ofInt("", paramArrayOfInt) });
    while (true)
    {
      this.mInitialized = false;
      return;
      this.mValues[0].setIntValues(paramArrayOfInt);
    }
  }

  public void setInterpolator(Interpolator paramInterpolator)
  {
    if (paramInterpolator != null)
    {
      this.mInterpolator = paramInterpolator;
      return;
    }
    this.mInterpolator = new LinearInterpolator();
  }

  public void setObjectValues(Object[] paramArrayOfObject)
  {
    if ((paramArrayOfObject == null) || (paramArrayOfObject.length == 0))
      return;
    if ((this.mValues == null) || (this.mValues.length == 0))
      setValues(new PropertyValuesHolder[] { PropertyValuesHolder.ofObject("", (TypeEvaluator)null, paramArrayOfObject) });
    while (true)
    {
      this.mInitialized = false;
      return;
      this.mValues[0].setObjectValues(paramArrayOfObject);
    }
  }

  public void setRepeatCount(int paramInt)
  {
    this.mRepeatCount = paramInt;
  }

  public void setRepeatMode(int paramInt)
  {
    this.mRepeatMode = paramInt;
  }

  public void setStartDelay(long paramLong)
  {
    this.mStartDelay = paramLong;
  }

  public void setValues(PropertyValuesHolder[] paramArrayOfPropertyValuesHolder)
  {
    int j = paramArrayOfPropertyValuesHolder.length;
    this.mValues = paramArrayOfPropertyValuesHolder;
    this.mValuesMap = new HashMap(j);
    int i = 0;
    while (i < j)
    {
      PropertyValuesHolder localPropertyValuesHolder = paramArrayOfPropertyValuesHolder[i];
      this.mValuesMap.put(localPropertyValuesHolder.getPropertyName(), localPropertyValuesHolder);
      i += 1;
    }
    this.mInitialized = false;
  }

  public void start()
  {
    start(false);
  }

  public String toString()
  {
    String str1 = "ValueAnimator@" + Integer.toHexString(hashCode());
    String str2 = str1;
    if (this.mValues != null)
    {
      int i = 0;
      while (true)
      {
        str2 = str1;
        if (i >= this.mValues.length)
          break;
        str1 = str1 + "\n    " + this.mValues[i].toString();
        i += 1;
      }
    }
    return str2;
  }

  private static class AnimationHandler extends Handler
  {
    public void handleMessage(Message paramMessage)
    {
      int j = 1;
      int i = 1;
      ArrayList localArrayList1 = (ArrayList)ValueAnimator.sAnimations.get();
      ArrayList localArrayList2 = (ArrayList)ValueAnimator.sDelayedAnims.get();
      switch (paramMessage.what)
      {
      default:
      case 0:
      case 1:
      }
      label114: long l;
      label157: 
      do
      {
        return;
        paramMessage = (ArrayList)ValueAnimator.sPendingAnimations.get();
        if ((localArrayList1.size() > 0) || (localArrayList2.size() > 0))
          i = 0;
        j = i;
        ValueAnimator localValueAnimator;
        if (paramMessage.size() > 0)
        {
          localObject = (ArrayList)paramMessage.clone();
          paramMessage.clear();
          k = ((ArrayList)localObject).size();
          j = 0;
          if (j < k)
          {
            localValueAnimator = (ValueAnimator)((ArrayList)localObject).get(j);
            if (localValueAnimator.mStartDelay != 0L)
              break label157;
            localValueAnimator.startAnimation();
          }
          while (true)
          {
            j += 1;
            break label114;
            break;
            localArrayList2.add(localValueAnimator);
          }
        }
        l = AnimationUtils.currentAnimationTimeMillis();
        Object localObject = (ArrayList)ValueAnimator.sReadyAnims.get();
        paramMessage = (ArrayList)ValueAnimator.sEndingAnims.get();
        int k = localArrayList2.size();
        i = 0;
        while (i < k)
        {
          localValueAnimator = (ValueAnimator)localArrayList2.get(i);
          if (localValueAnimator.delayedAnimationFrame(l))
            ((ArrayList)localObject).add(localValueAnimator);
          i += 1;
        }
        k = ((ArrayList)localObject).size();
        if (k > 0)
        {
          i = 0;
          while (i < k)
          {
            localValueAnimator = (ValueAnimator)((ArrayList)localObject).get(i);
            localValueAnimator.startAnimation();
            ValueAnimator.access$802(localValueAnimator, true);
            localArrayList2.remove(localValueAnimator);
            i += 1;
          }
          ((ArrayList)localObject).clear();
        }
        i = localArrayList1.size();
        k = 0;
        while (k < i)
        {
          localObject = (ValueAnimator)localArrayList1.get(k);
          if (((ValueAnimator)localObject).animationFrame(l))
            paramMessage.add(localObject);
          if (localArrayList1.size() == i)
          {
            k += 1;
            continue;
          }
          i -= 1;
          paramMessage.remove(localObject);
        }
        if (paramMessage.size() <= 0)
          continue;
        i = 0;
        while (i < paramMessage.size())
        {
          ((ValueAnimator)paramMessage.get(i)).endAnimation();
          i += 1;
        }
        paramMessage.clear();
      }
      while ((j == 0) || ((localArrayList1.isEmpty()) && (localArrayList2.isEmpty())));
      sendEmptyMessageDelayed(1, Math.max(0L, ValueAnimator.sFrameDelay - (AnimationUtils.currentAnimationTimeMillis() - l)));
    }
  }

  public static abstract interface AnimatorUpdateListener
  {
    public abstract void onAnimationUpdate(ValueAnimator paramValueAnimator);
  }
}

/* Location:           C:\Users\xuetong\Desktop\dazhongdianping7.9.6\ProjectSrc\classes-dex2jar.jar
 * Qualified Name:     com.nineoldandroids.animation.ValueAnimator
 * JD-Core Version:    0.6.0
 */
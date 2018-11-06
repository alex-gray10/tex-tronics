package andrewpeltier.smartglovefragments.visualize;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.ArrayRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import andrewpeltier.smartglovefragments.R;
import andrewpeltier.smartglovefragments.io.SmartGloveInterface;


public class ExerciseView extends LinearLayout implements SmartGloveInterface
{
    private Paint gradientPaint;
    private int[] currentGradient;

    private TextView exerciseDescription;
    private TextView exerciseName;
    private ImageView exerciseImage;

    private ArgbEvaluator evaluator;

    public ExerciseView(Context context) {
        super(context);
    }

    public ExerciseView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExerciseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        evaluator = new ArgbEvaluator();

        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setWillNotDraw(false);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        inflate(getContext(), R.layout.instructions_layout, this);

        exerciseDescription = findViewById(R.id.exercise_description);
        exerciseImage = findViewById(R.id.exercise_gif);
        exerciseName = findViewById(R.id.exercise_title);
    }

    private void initGradient() {
        float centerX = getWidth() * 0.5f;
        Shader gradient = new LinearGradient(
                centerX, 0, centerX, getHeight(),
                currentGradient, null,
                Shader.TileMode.MIRROR);
        gradientPaint.setShader(gradient);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (currentGradient != null) {
            initGradient();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), gradientPaint);
        super.onDraw(canvas);
    }

    public void setChoice(Exercise exercise) {
        Choice choice = exercise.getChoice();
        currentGradient = choiceToGradient(choice);
        if (getWidth() != 0 && getHeight() != 0) {
            initGradient();
        }
        exerciseDescription.setText(choice.getDisplayName());
        exerciseName.setText(exercise.getTextName());
        Glide.with(getContext()).load(choiceToIcon(choice)).into(exerciseImage);
        invalidate();

        exerciseImage.animate()
                .scaleX(1f).scaleY(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(300)
                .start();
    }

    public void onScroll(float fraction, Exercise oldF, Exercise newF) {
        exerciseImage.setScaleX(fraction);
        exerciseImage.setScaleY(fraction);
        currentGradient = mix(fraction,
                choiceToGradient(newF.getChoice()),
                choiceToGradient(oldF.getChoice()));
        initGradient();
        invalidate();
    }

    private int[] mix(float fraction, int[] c1, int[] c2) {
        return new int[]{
                (Integer) evaluator.evaluate(fraction, c1[0], c2[0]),
                (Integer) evaluator.evaluate(fraction, c1[1], c2[1]),
                (Integer) evaluator.evaluate(fraction, c1[2], c2[2])
        };
    }

    private int[] choiceToGradient(Choice choice) {
        switch (choice) {
            case FINGER_TAP:
                return colors(R.array.gradientFingerTap);
            case CLOSED_GRIP:
                return colors(R.array.gradientClosedGrip);
            case HAND_FLIP:
                return colors(R.array.gradientHandFlip);
            case SCREEN_TAP:
                return colors(R.array.gradientScreenTap);
            case HEEL_TAP:
                return colors(R.array.gradientHeelTap);
            case TOE_TAP:
                return colors(R.array.gradientToeTap);
            case FOOT_STOMP:
                return colors(R.array.gradientFootStomp);
            case WALK_STEPS:
                return colors(R.array.gradientWalkSteps);
            default:
                throw new IllegalArgumentException();
        }
    }

    private int choiceToIcon(Choice choice) {
        switch (choice) {
            case FINGER_TAP:
                return InstructionsImage.FINGER_TAP_GIF;
            case CLOSED_GRIP:
                return InstructionsImage.CLOSED_GRIP_GIF;
            case HAND_FLIP:
                return InstructionsImage.HAND_FLIP_GIF;
            case SCREEN_TAP:
                return InstructionsImage.SCREEN_TAP_GIF;
            case HEEL_TAP:
                return InstructionsImage.HEEL_TAP_GIF;
            case TOE_TAP:
                return InstructionsImage.TOE_TAP_GIF;
            case FOOT_STOMP:
                return InstructionsImage.FOOT_STOMP_GIF;
            case WALK_STEPS:
                return InstructionsImage.WALK_STEPS_GIF;
            default:
                throw new IllegalArgumentException();
        }
    }

    private int[] colors(@ArrayRes int res) {
        return getContext().getResources().getIntArray(res);
    }
}
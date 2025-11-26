package com.example.traduccioncotorra;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TraduccionOverlayView extends View {

    private static final String TAG = "OVERLAY_VIEW";

    private Paint backgroundPaint;
    private Paint textPaint;
    private Paint borderPaint;

    private List<TranslationBox> translationBoxes = new ArrayList<>();

    public TraduccionOverlayView(Context context) {
        super(context);
        init();
    }

    public TraduccionOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Fondo semi-transparente para las cajas de traducción
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#E6000000")); // Negro 90% opaco
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        // Texto de la traducción
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);

        // Borde de las cajas
        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#FF6B35")); // Naranja
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setAntiAlias(true);
    }

    /**
     * Actualiza las cajas de traducción
     */
    public void setTranslationBoxes(List<TranslationBox> boxes) {
        this.translationBoxes = boxes;
        invalidate(); // Redibujar
    }

    /**
     * Limpia todas las traducciones
     */
    public void clear() {
        translationBoxes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (TranslationBox box : translationBoxes) {
            // Ajustar coordenadas al tamaño de la vista
            RectF adjustedRect = adjustRect(box.boundingBox);

            // Dibujar fondo redondeado
            canvas.drawRoundRect(adjustedRect, 20f, 20f, backgroundPaint);

            // Dibujar borde
            canvas.drawRoundRect(adjustedRect, 20f, 20f, borderPaint);

            // Dibujar texto traducido (con saltos de línea si es necesario)
            drawMultilineText(canvas, box.translatedText, adjustedRect);
        }
    }

    /**
     * Ajusta el rectángulo al tamaño de la vista
     */
    private RectF adjustRect(Rect originalRect) {
        float scaleX = (float) getWidth() / 1080f; // Asume tamaño de imagen 1080p
        float scaleY = (float) getHeight() / 1920f;

        return new RectF(
                originalRect.left * scaleX,
                originalRect.top * scaleY,
                originalRect.right * scaleX,
                originalRect.bottom * scaleY
        );
    }

    /**
     * Dibuja texto con saltos de línea automáticos
     */
    private void drawMultilineText(Canvas canvas, String text, RectF rect) {
        float padding = 20f;
        float x = rect.left + padding;
        float y = rect.top + padding + textPaint.getTextSize();

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float maxWidth = rect.width() - (padding * 2);

        for (String word : words) {
            String testLine = line + word + " ";
            float testWidth = textPaint.measureText(testLine);

            if (testWidth > maxWidth && line.length() > 0) {
                // Dibujar línea actual
                canvas.drawText(line.toString().trim(), x, y, textPaint);
                y += textPaint.getTextSize() + 10f;
                line = new StringBuilder(word + " ");
            } else {
                line.append(word).append(" ");
            }
        }

        // Dibujar última línea
        if (line.length() > 0) {
            canvas.drawText(line.toString().trim(), x, y, textPaint);
        }
    }

    /**
     * Clase para almacenar una caja de traducción
     */
    public static class TranslationBox {
        public Rect boundingBox;
        public String originalText;
        public String translatedText;

        public TranslationBox(Rect boundingBox, String originalText, String translatedText) {
            this.boundingBox = boundingBox;
            this.originalText = originalText;
            this.translatedText = translatedText;
        }
    }
}
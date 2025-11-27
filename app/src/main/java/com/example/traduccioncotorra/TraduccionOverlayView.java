package com.example.traduccioncotorra;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TraduccionOverlayView extends View {

    private static final String TAG = "OVERLAY_VIEW";

    private Paint backgroundPaint;
    private Paint textPaint;
    private Paint borderPaint;

    // ✅ Paint para el recuadro de captura
    private Paint captureBoxPaint;
    private Paint captureBoxBorderPaint;
    private Paint handlePaint;
    private Paint analyzingPaint;

    private List<TranslationBox> translationBoxes = new ArrayList<>();

    private int imageWidth = 1080;
    private int imageHeight = 1920;

    // ✅ Recuadro de captura ajustable
    private RectF captureBox;
    private static final float MIN_BOX_SIZE = 300f; // ✅ Aumentado de 200f a 300f
    private static final float HANDLE_SIZE = 70f; // ✅ Aumentado de 60f a 70f

    private boolean isDragging = false;
    private boolean isResizing = false;
    private boolean isAnalyzing = false; // ✅ NUEVO: Estado de análisis
    private ResizeHandle activeHandle = ResizeHandle.NONE;
    private float lastTouchX, lastTouchY;

    // ✅ Listener para notificar cambios en el recuadro
    private OnBoxChangedListener onBoxChangedListener;

    private enum ResizeHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }

    public interface OnBoxChangedListener {
        void onBoxChanged();
    }

    public TraduccionOverlayView(Context context) {
        super(context);
        init();
    }

    public TraduccionOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint para traducciones
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#E6000000"));
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#FF6B35"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setAntiAlias(true);

        // ✅ Paint para el recuadro de captura
        captureBoxPaint = new Paint();
        captureBoxPaint.setColor(Color.parseColor("#4000FF00")); // Verde semi-transparente
        captureBoxPaint.setStyle(Paint.Style.FILL);
        captureBoxPaint.setAntiAlias(true);

        captureBoxBorderPaint = new Paint();
        captureBoxBorderPaint.setColor(Color.parseColor("#00FF00")); // Verde brillante
        captureBoxBorderPaint.setStyle(Paint.Style.STROKE);
        captureBoxBorderPaint.setStrokeWidth(6f);
        captureBoxBorderPaint.setAntiAlias(true);
        captureBoxBorderPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{20, 10}, 0));

        handlePaint = new Paint();
        handlePaint.setColor(Color.parseColor("#00FF00"));
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);

        // ✅ NUEVO: Paint para indicador de análisis
        analyzingPaint = new Paint();
        analyzingPaint.setColor(Color.parseColor("#FFFF00")); // Amarillo
        analyzingPaint.setStyle(Paint.Style.STROKE);
        analyzingPaint.setStrokeWidth(8f);
        analyzingPaint.setAntiAlias(true);

        // Inicializar recuadro en el centro (se ajustará en onSizeChanged)
        captureBox = new RectF(0, 0, 400, 300);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // ✅ MEJORADO: Recuadro más grande por defecto
        float boxWidth = Math.min(w * 0.8f, 700f); // Aumentado de 0.7f a 0.8f
        float boxHeight = Math.min(h * 0.5f, 500f); // Aumentado de 0.4f a 0.5f
        float left = (w - boxWidth) / 2;
        float top = (h - boxHeight) / 2;

        captureBox = new RectF(left, top, left + boxWidth, top + boxHeight);
    }

    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }

    public void setTranslationBoxes(List<TranslationBox> boxes) {
        this.translationBoxes = boxes;
        invalidate();
    }

    public void clear() {
        translationBoxes.clear();
        invalidate();
    }

    /**
     * ✅ NUEVO: Establecer estado de análisis
     */
    public void setAnalyzing(boolean analyzing) {
        this.isAnalyzing = analyzing;
        invalidate();
    }

    /**
     * ✅ Configurar listener para cambios en el recuadro
     */
    public void setOnBoxChangedListener(OnBoxChangedListener listener) {
        this.onBoxChangedListener = listener;
    }

    /**
     * ✅ Obtener el recuadro de captura en coordenadas de imagen
     */
    public Rect getCaptureBoxInImageCoords() {
        float scaleX = (float) imageWidth / (float) getWidth();
        float scaleY = (float) imageHeight / (float) getHeight();

        return new Rect(
                (int) (captureBox.left * scaleX),
                (int) (captureBox.top * scaleY),
                (int) (captureBox.right * scaleX),
                (int) (captureBox.bottom * scaleY)
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = getHandleAt(x, y);

                if (activeHandle != ResizeHandle.NONE) {
                    isResizing = true;
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                } else if (captureBox.contains(x, y)) {
                    isDragging = true;
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    resizeCaptureBox(x, y);
                    invalidate();
                    return true;
                } else if (isDragging) {
                    moveCaptureBox(x, y);
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging || isResizing) {
                    // ✅ Notificar que el recuadro cambió
                    if (onBoxChangedListener != null) {
                        onBoxChangedListener.onBoxChanged();
                    }
                    invalidate();
                }

                isDragging = false;
                isResizing = false;
                activeHandle = ResizeHandle.NONE;
                break;
        }

        return super.onTouchEvent(event);
    }

    private ResizeHandle getHandleAt(float x, float y) {
        float tolerance = HANDLE_SIZE;

        // Esquina superior izquierda
        if (Math.abs(x - captureBox.left) < tolerance && Math.abs(y - captureBox.top) < tolerance) {
            return ResizeHandle.TOP_LEFT;
        }
        // Esquina superior derecha
        if (Math.abs(x - captureBox.right) < tolerance && Math.abs(y - captureBox.top) < tolerance) {
            return ResizeHandle.TOP_RIGHT;
        }
        // Esquina inferior izquierda
        if (Math.abs(x - captureBox.left) < tolerance && Math.abs(y - captureBox.bottom) < tolerance) {
            return ResizeHandle.BOTTOM_LEFT;
        }
        // Esquina inferior derecha
        if (Math.abs(x - captureBox.right) < tolerance && Math.abs(y - captureBox.bottom) < tolerance) {
            return ResizeHandle.BOTTOM_RIGHT;
        }

        return ResizeHandle.NONE;
    }

    private void moveCaptureBox(float x, float y) {
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;

        RectF newBox = new RectF(captureBox);
        newBox.offset(dx, dy);

        // Limitar dentro de la vista
        if (newBox.left >= 0 && newBox.right <= getWidth() &&
                newBox.top >= 0 && newBox.bottom <= getHeight()) {
            captureBox = newBox;
            lastTouchX = x;
            lastTouchY = y;
        }
    }

    private void resizeCaptureBox(float x, float y) {
        RectF newBox = new RectF(captureBox);

        switch (activeHandle) {
            case TOP_LEFT:
                newBox.left = Math.min(x, captureBox.right - MIN_BOX_SIZE);
                newBox.top = Math.min(y, captureBox.bottom - MIN_BOX_SIZE);
                break;
            case TOP_RIGHT:
                newBox.right = Math.max(x, captureBox.left + MIN_BOX_SIZE);
                newBox.top = Math.min(y, captureBox.bottom - MIN_BOX_SIZE);
                break;
            case BOTTOM_LEFT:
                newBox.left = Math.min(x, captureBox.right - MIN_BOX_SIZE);
                newBox.bottom = Math.max(y, captureBox.top + MIN_BOX_SIZE);
                break;
            case BOTTOM_RIGHT:
                newBox.right = Math.max(x, captureBox.left + MIN_BOX_SIZE);
                newBox.bottom = Math.max(y, captureBox.top + MIN_BOX_SIZE);
                break;
        }

        // Limitar dentro de la vista
        newBox.left = Math.max(0, newBox.left);
        newBox.top = Math.max(0, newBox.top);
        newBox.right = Math.min(getWidth(), newBox.right);
        newBox.bottom = Math.min(getHeight(), newBox.bottom);

        // Validar tamaño mínimo
        if (newBox.width() >= MIN_BOX_SIZE && newBox.height() >= MIN_BOX_SIZE) {
            captureBox = newBox;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Dibujar el recuadro de captura con indicador de análisis
        if (isAnalyzing) {
            // ✅ Borde amarillo parpadeante cuando está analizando
            canvas.drawRect(captureBox, analyzingPaint);
        } else {
            canvas.drawRect(captureBox, captureBoxPaint);
        }
        canvas.drawRect(captureBox, captureBoxBorderPaint);

        // 2. Dibujar handles (círculos en las esquinas)
        drawHandle(canvas, captureBox.left, captureBox.top);
        drawHandle(canvas, captureBox.right, captureBox.top);
        drawHandle(canvas, captureBox.left, captureBox.bottom);
        drawHandle(canvas, captureBox.right, captureBox.bottom);

        // 3. Dibujar texto de instrucción
        Paint instructionPaint = new Paint();
        instructionPaint.setColor(Color.WHITE);
        instructionPaint.setTextSize(32f);
        instructionPaint.setAntiAlias(true);
        instructionPaint.setTextAlign(Paint.Align.CENTER);
        instructionPaint.setShadowLayer(8f, 0f, 0f, Color.BLACK);

        String instructionText = isAnalyzing ? "🔍 Analizando..." : "Apunta el texto aquí";
        canvas.drawText(instructionText,
                captureBox.centerX(),
                captureBox.top - 20f,
                instructionPaint);

        // 4. Dibujar traducciones dentro del recuadro
        if (!translationBoxes.isEmpty()) {
            drawTranslationsInBox(canvas);
        }
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, HANDLE_SIZE / 2, handlePaint);

        // Borde del handle
        Paint handleBorderPaint = new Paint(handlePaint);
        handleBorderPaint.setStyle(Paint.Style.STROKE);
        handleBorderPaint.setStrokeWidth(3f);
        handleBorderPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, HANDLE_SIZE / 2, handleBorderPaint);
    }

    /**
     * ✅ Dibujar todas las traducciones dentro del recuadro
     */
    private void drawTranslationsInBox(Canvas canvas) {
        StringBuilder allTranslations = new StringBuilder();

        for (TranslationBox box : translationBoxes) {
            if (allTranslations.length() > 0) {
                allTranslations.append("\n\n");
            }
            allTranslations.append(box.translatedText);
        }

        if (allTranslations.length() == 0) {
            return;
        }

        // Fondo semi-transparente
        canvas.drawRoundRect(captureBox, 20f, 20f, backgroundPaint);

        // Dibujar texto centrado con saltos de línea
        drawCenteredMultilineText(canvas, allTranslations.toString(), captureBox);
    }

    /**
     * ✅ Dibujar texto multilínea centrado
     */
    private void drawCenteredMultilineText(Canvas canvas, String text, RectF rect) {
        float padding = 30f;
        float maxWidth = rect.width() - (padding * 2);

        String[] lines = text.split("\n");
        List<String> wrappedLines = new ArrayList<>();

        // Dividir líneas largas
        for (String line : lines) {
            if (textPaint.measureText(line) <= maxWidth) {
                wrappedLines.add(line);
            } else {
                // Dividir palabras
                String[] words = line.split(" ");
                StringBuilder currentLine = new StringBuilder();

                for (String word : words) {
                    String testLine = currentLine + (currentLine.length() > 0 ? " " : "") + word;

                    if (textPaint.measureText(testLine) <= maxWidth) {
                        currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
                    } else {
                        if (currentLine.length() > 0) {
                            wrappedLines.add(currentLine.toString());
                        }
                        currentLine = new StringBuilder(word);
                    }
                }

                if (currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                }
            }
        }

        // Calcular altura total
        float lineHeight = textPaint.getTextSize() + 12f;
        float totalHeight = wrappedLines.size() * lineHeight;

        // Centrar verticalmente
        float startY = rect.centerY() - (totalHeight / 2) + textPaint.getTextSize();

        // Dibujar cada línea
        for (String line : wrappedLines) {
            canvas.drawText(line, rect.centerX(), startY, textPaint);
            startY += lineHeight;
        }
    }

    /**
     * ✅ Clase para almacenar una caja de traducción
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
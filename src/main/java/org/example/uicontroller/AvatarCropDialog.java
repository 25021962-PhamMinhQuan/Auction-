package org.example.uicontroller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

/**
 * Dialog crop ảnh đại diện kiểu Facebook.
 *
 * <p>Cách dùng:</p>
 * <pre>
 *   AvatarCropDialog.show(ownerStage, rawImage, croppedImage -> {
 *       // croppedImage là WritableImage vuông đã cắt sẵn
 *   });
 * </pre>
 */
public class AvatarCropDialog {

    /** Kích thước vùng xem trước (crop preview) */
    private static final int PREVIEW_SIZE = 300;
    /** Kích thước ảnh export cuối cùng (pixel) */
    private static final int EXPORT_SIZE  = 400;

    // ── Trạng thái drag ──────────────────────────────────────────────────────
    private double dragStartX, dragStartY;
    private double imgOffsetX, imgOffsetY;
    private double scale = 1.0;
    private double imgDrawW, imgDrawH;

    /**
     * Mở dialog crop modal.
     *
     * @param owner    cửa sổ cha
     * @param source   ảnh gốc (chưa crop)
     * @param callback nhận {@link WritableImage} đã crop, hoặc {@code null} nếu user huỷ
     */
    public static void show(Stage owner, Image source, Consumer<WritableImage> callback) {
        new AvatarCropDialog().openDialog(owner, source, callback);
    }

    private void openDialog(Stage owner, Image source, Consumer<WritableImage> callback) {

        // ── Canvas vẽ preview ─────────────────────────────────────────────────
        Canvas canvas = new Canvas(PREVIEW_SIZE, PREVIEW_SIZE);

        // Tỷ lệ ảnh ban đầu: fit vừa khung tròn
        double srcW = source.getWidth();
        double srcH = source.getHeight();
        double ratio = Math.max(PREVIEW_SIZE / srcW, PREVIEW_SIZE / srcH);
        imgDrawW  = srcW * ratio;
        imgDrawH  = srcH * ratio;
        imgOffsetX = (PREVIEW_SIZE - imgDrawW) / 2.0;
        imgOffsetY = (PREVIEW_SIZE - imgDrawH) / 2.0;
        scale = 1.0;

        // ── Lớp overlay tròn (hiệu ứng mờ viền như Facebook) ─────────────────
        Circle cropCircle = new Circle(PREVIEW_SIZE / 2.0, PREVIEW_SIZE / 2.0,
                PREVIEW_SIZE / 2.0 - 4);
        cropCircle.setFill(Color.TRANSPARENT);
        cropCircle.setStroke(Color.web("#fbbf24"));
        cropCircle.setStrokeWidth(2.5);
        cropCircle.setStrokeType(StrokeType.INSIDE);
        cropCircle.setMouseTransparent(true);

        StackPane cropPane = new StackPane(canvas, cropCircle);
        cropPane.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
        cropPane.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        cropPane.setStyle(
                "-fx-background-color: #111827;" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: #374151;" +
                        "-fx-border-radius: 999;" +
                        "-fx-border-width: 1;"
        );
        // Cắt khung tròn
        Circle clipShape = new Circle(PREVIEW_SIZE / 2.0, PREVIEW_SIZE / 2.0, PREVIEW_SIZE / 2.0);
        cropPane.setClip(clipShape);

        // Vẽ lần đầu
        redraw(canvas, source);

        // ── Drag để di chuyển ảnh ────────────────────────────────────────────
        canvas.setOnMousePressed(e -> { dragStartX = e.getX(); dragStartY = e.getY(); });
        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            dragStartX = e.getX();
            dragStartY = e.getY();
            imgOffsetX = clampOffset(imgOffsetX + dx, imgDrawW, PREVIEW_SIZE);
            imgOffsetY = clampOffset(imgOffsetY + dy, imgDrawH, PREVIEW_SIZE);
            redraw(canvas, source);
        });

        // ── Scroll để zoom ───────────────────────────────────────────────────
        canvas.setOnScroll(e -> {
            double delta = e.getDeltaY() > 0 ? 0.05 : -0.05;
            double newScale = Math.max(0.5, Math.min(4.0, scale + delta));

            // Zoom tại vị trí con trỏ chuột
            double mouseX = e.getX();
            double mouseY = e.getY();
            double newW = imgDrawW / scale * newScale;
            double newH = imgDrawH / scale * newScale;
            imgOffsetX = mouseX - (mouseX - imgOffsetX) * (newScale / scale);
            imgOffsetY = mouseY - (mouseY - imgOffsetY) * (newScale / scale);
            imgDrawW = newW;
            imgDrawH = newH;
            scale = newScale;
            imgOffsetX = clampOffset(imgOffsetX, imgDrawW, PREVIEW_SIZE);
            imgOffsetY = clampOffset(imgOffsetY, imgDrawH, PREVIEW_SIZE);
            redraw(canvas, source);
        });

        // ── Slider zoom ──────────────────────────────────────────────────────
        Slider zoomSlider = new Slider(0.5, 4.0, 1.0);
        zoomSlider.setPrefWidth(PREVIEW_SIZE);
        zoomSlider.setStyle("-fx-accent: #fbbf24;");
        zoomSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double newScale = newV.doubleValue();
            double cx = PREVIEW_SIZE / 2.0;
            double cy = PREVIEW_SIZE / 2.0;
            double newW = imgDrawW / scale * newScale;
            double newH = imgDrawH / scale * newScale;
            imgOffsetX = cx - (cx - imgOffsetX) * (newScale / scale);
            imgOffsetY = cy - (cy - imgOffsetY) * (newScale / scale);
            imgDrawW = newW;
            imgDrawH = newH;
            scale = newScale;
            imgOffsetX = clampOffset(imgOffsetX, imgDrawW, PREVIEW_SIZE);
            imgOffsetY = clampOffset(imgOffsetY, imgDrawH, PREVIEW_SIZE);
            redraw(canvas, source);
        });

        // ── Nút Xác nhận / Huỷ ───────────────────────────────────────────────
        Button confirmBtn = new Button("✔  Xác nhận");
        confirmBtn.setStyle(
                "-fx-background-color: #fbbf24; -fx-text-fill: #111827;" +
                        "-fx-font-weight: bold; -fx-font-size: 13px;" +
                        "-fx-background-radius: 8; -fx-padding: 8 20;"
        );

        Button cancelBtn = new Button("✕  Huỷ");
        cancelBtn.setStyle(
                "-fx-background-color: #374151; -fx-text-fill: #e5e7eb;" +
                        "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 8 20;"
        );

        HBox btnRow = new HBox(12, cancelBtn, confirmBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── Hint text ─────────────────────────────────────────────────────────
        Label hint = new Label("Kéo để di chuyển  •  Cuộn chuột hoặc kéo thanh để phóng to/thu nhỏ");
        hint.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        // ── Layout ────────────────────────────────────────────────────────────
        Label title = new Label("Chỉnh sửa ảnh đại diện");
        title.setStyle("-fx-text-fill: #f9fafb; -fx-font-size: 16px; -fx-font-weight: bold;");

        VBox root = new VBox(16, title, cropPane, hint, zoomSlider, btnRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 14;");
        root.setMaxWidth(400);

        // ── Stage ─────────────────────────────────────────────────────────────
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);

        Scene scene = new Scene(root, Color.TRANSPARENT);
        dialog.setScene(scene);

        // Nút Huỷ
        cancelBtn.setOnAction(e -> { callback.accept(null); dialog.close(); });

        // Nút Xác nhận — render ra WritableImage EXPORT_SIZE x EXPORT_SIZE
        confirmBtn.setOnAction(e -> {
            WritableImage output = exportCroppedImage(source);
            callback.accept(output);
            dialog.close();
        });

        dialog.showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Vẽ ảnh lên canvas với vị trí và kích thước hiện tại */
    private void redraw(Canvas canvas, Image source) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
        gc.setFill(Color.web("#111827"));
        gc.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
        gc.drawImage(source, imgOffsetX, imgOffsetY, imgDrawW, imgDrawH);
    }

    /**
     * Xuất ảnh crop thành {@link WritableImage} vuông {@value EXPORT_SIZE}px.
     * Tỷ lệ map từ vùng hiển thị (PREVIEW_SIZE) ra kích thước nguồn thực tế.
     */
    private WritableImage exportCroppedImage(Image source) {
        // Tính vùng crop trên ảnh gốc (pixel ảnh nguồn)
        double scaleToSrc = source.getWidth() / imgDrawW;
        double srcX = -imgOffsetX * scaleToSrc;
        double srcY = -imgOffsetY * scaleToSrc;
        double srcSize = PREVIEW_SIZE * scaleToSrc;

        // Clamp để không vượt biên
        srcX = Math.max(0, Math.min(srcX, source.getWidth()  - srcSize));
        srcY = Math.max(0, Math.min(srcY, source.getHeight() - srcSize));
        srcSize = Math.min(srcSize, Math.min(source.getWidth() - srcX, source.getHeight() - srcY));

        // Render ra Canvas nội bộ rồi snapshot
        Canvas exportCanvas = new Canvas(EXPORT_SIZE, EXPORT_SIZE);
        GraphicsContext gc = exportCanvas.getGraphicsContext2D();

        // Clip tròn
        gc.beginPath();
        gc.arc(EXPORT_SIZE / 2.0, EXPORT_SIZE / 2.0,
                EXPORT_SIZE / 2.0, EXPORT_SIZE / 2.0, 0, 360);
        gc.closePath();
        gc.clip();

        gc.drawImage(source,
                srcX, srcY, srcSize, srcSize,
                0, 0, EXPORT_SIZE, EXPORT_SIZE);

        WritableImage wi = new WritableImage(EXPORT_SIZE, EXPORT_SIZE);
        exportCanvas.snapshot(null, wi);
        return wi;
    }

    /**
     * Đảm bảo ảnh không bị kéo ra ngoài vùng nhìn thấy (luôn che kín khung tròn).
     * offset âm nghĩa là ảnh dịch sang trái / lên trên.
     */
    private double clampOffset(double offset, double imgSize, double viewSize) {
        // Ảnh phải phủ toàn bộ viewSize
        double maxOffset = 0;
        double minOffset = viewSize - imgSize;
        return Math.max(minOffset, Math.min(maxOffset, offset));
    }
}
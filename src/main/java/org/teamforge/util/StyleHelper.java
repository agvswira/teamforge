package org.teamforge.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import org.teamforge.model.Task;
import org.teamforge.model.TaskStatus;

/**
 * Method static untuk membuat avatar, badge status, dan format tanggal.
 * Warna badge diatur di teamforge.css lewat nama styleClass.
 */
public class StyleHelper {
    private static final String[] BULAN = {
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };

    private StyleHelper() {
    }

    // Avatar: lingkaran berisi inisial nama, warnanya tetap untuk nama yang sama
    public static Label avatar(String name) {
        String[] words = name.trim().split(" ");
        String initials = words[0].substring(0, 1);
        if (words.length > 1) {
            initials = initials + words[1].substring(0, 1);
        }
        int total = 0;
        for (char c : name.toCharArray()) {
            total = total + c;
        }
        Label label = new Label(initials.toUpperCase());
        label.getStyleClass().add("avatar");
        label.getStyleClass().add("avatar-" + (total % 6));
        label.setTooltip(new Tooltip(name));
        return label;
    }

    // Beberapa avatar berjajar, misalnya untuk penerima tugas atau anggota project
    public static HBox avatarGroup(List<String> names) {
        HBox box = new HBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        for (String name : names) {
            box.getChildren().add(avatar(name));
        }
        return box;
    }

    // Badge dengan latar berwarna, contoh variant: "brand", "neutral", "overdue", "blocked"
    public static Label chip(String text, String variant) {
        Label label = new Label(text);
        label.getStyleClass().add("chip");
        label.getStyleClass().add("chip-" + variant);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    public static String variantOf(TaskStatus status) {
        return switch (status) {
            case TODO -> "todo";
            case IN_PROGRESS -> "progress";
            case DONE -> "done";
        };
    }

    public static Label statusChip(TaskStatus status) {
        return chip(status.getLabel(), variantOf(status));
    }

    public static HBox statusText(TaskStatus status) {
        return dotText(status.getLabel(), variantOf(status));
    }

    public static HBox dotText(String text, String variant) {
        Region dot = new Region();
        dot.getStyleClass().add("dot");
        HBox box = new HBox(6, dot, new Label(text));
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("status-text");
        box.getStyleClass().add("status-" + variant);
        return box;
    }

    public static Label priority(Task task) {
        Label label = new Label(task.getPriorityLabel());
        if (task.getPriority() == Task.PRIORITY_HIGH) {
            label.getStyleClass().add("text-overdue");
        } else if (task.getPriority() == Task.PRIORITY_MEDIUM) {
            label.getStyleClass().add("text-blocked");
        } else {
            label.getStyleClass().add("muted");
        }
        return label;
    }

    // Contoh hasil: 12 Des 2026
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.getDayOfMonth() + " " + BULAN[date.getMonthValue() - 1] + " " + date.getYear();
    }

    // Contoh hasil: 12 Des
    public static String formatShortDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.getDayOfMonth() + " " + BULAN[date.getMonthValue() - 1];
    }

    public static long daysFromToday(LocalDate date) {
        return ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    // Menampilkan atau menyembunyikan komponen beserta ruangnya
    public static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    public static void showMessage(Label label, String text) {
        label.setText(text);
        setShown(label, true);
    }

    public static void hideMessage(Label label) {
        label.setText("");
        setShown(label, false);
    }
}

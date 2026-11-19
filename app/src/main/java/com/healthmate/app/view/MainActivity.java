package com.healthmate.app.view;
import android.widget.ProgressBar;

import com.healthmate.app.R;
import com.healthmate.app.model.HealthRecord;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import com.healthmate.app.controller.AuthController;
import com.healthmate.app.controller.HealthController;
import com.healthmate.app.controller.HistoryController;
import com.healthmate.app.model.User;
import com.healthmate.app.ui.Ui;
import android.app.DatePickerDialog;
import java.util.function.IntConsumer;
import android.text.Editable;
import android.text.TextWatcher;
import com.healthmate.app.ui.TrendChartView;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import android.os.Build;
import android.window.OnBackInvokedDispatcher;

public class MainActivity extends Activity {
    private static final int STEP_GOAL = 10_000;
    private static final int CALORIE_GOAL = 600;
    private static final int WATER_GOAL = 2_500;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Controllers (the "C" in MVC) - the Activity is the View and never touches the database directly
    private AuthController authController;
    private HealthController healthController;
    private HistoryController historyController;
    private User currentUser;
    private String activeTab = "today";
    private String activeAuthTab = "signin";
    private LocalDate selectedDate = LocalDate.now();
    private String editingDate;
    private int steps;
    private int calories;
    private int waterMl;
    private EditText noteInput;
    private TextView formStatus;
    private String historyQuery = "";
    private String historyRange = "all";
    private boolean oldestFirst;
    private EditText historySearch;
    private LinearLayout historyResults;
    private boolean monthlyTrends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = new AuthController(this);
        healthController = new HealthController(this);
        historyController = new HistoryController();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
        styleSystemBars();
        openSavedSession();
    }

    private void styleSystemBars() {
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void openSavedSession() {
        currentUser = authController.getSessionUser();
        if (currentUser == null) {
            authController.logout();
            showAuth();
        } else {
            renderApp();
        }
    }

    private void showAuth() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        scroll.setClipToPadding(false);

        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom() );
            return insets;
        });

        LinearLayout page = Ui.vertical(this);
        page.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        page.setPadding(Ui.dp(this, 24), Ui.dp(this, 22), Ui.dp(this, 24), Ui.dp(this, 28));
        scroll.addView(page);

        addBrandMark(page, true);
        addCenterText(page, "HealthMate", 25, Ui.TEXT, true, 5);
        addCenterText(page, "Personal Health & Wellness Tracker", 14, Ui.MUTED, false, 18);

        LinearLayout switcher = Ui.horizontal(this);
        switcher.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        switcher.setBackground(Ui.rounded(Color.rgb(231, 236, 243), Ui.dp(this, 15), Color.TRANSPARENT, 0));
        TextView signIn = Ui.button(this, " Sign In",
                "signin".equals(activeAuthTab) ? Ui.WHITE : Color.TRANSPARENT,
                "signin".equals(activeAuthTab) ? Ui.TEXT : Ui.MUTED, 12, "signin".equals(activeAuthTab));
        TextView createAccount = Ui.button(this, "Create Account",
                "register".equals(activeAuthTab) ? Ui.WHITE : Color.TRANSPARENT,
                "register".equals(activeAuthTab) ? Ui.TEXT : Ui.MUTED, 12, "register".equals(activeAuthTab));
        switcher.addView(signIn, Ui.weighted(this, 42, 1));
        switcher.addView(createAccount, Ui.weighted(this, 42, 1));
        page.addView(switcher, Ui.margins(this, -1, -2, 0, 0, 0, 16));
        signIn.setOnClickListener(view -> {
            activeAuthTab = "signin";
            showAuth();
        });
        createAccount.setOnClickListener(view -> {
            activeAuthTab = "register";
            showAuth();
        });

        if ("register".equals(activeAuthTab)) {
            buildRegisterForm(page);
        } else {
            buildSignInForm(page);
        }

        setContentView(scroll);
    }

    private void addBrandMark(LinearLayout page, boolean large) {
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.healthmate_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int size = large ? 96 : 40;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(this, size), Ui.dp(this, size));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = Ui.dp(this, large ? 10 : 0);
        page.addView(logo, params);
    }

    private void addCenterText(LinearLayout page, String value, int size,
                               int color, boolean bold, int bottomMargin) {
        TextView label = Ui.text(this, value, size, color, bold);
        label.setGravity(Gravity.CENTER);
        page.addView(label, Ui.margins(this, -1, -2, 0, 0, 0, bottomMargin));
    }

    // SIGN IN FORM

    private void buildSignInForm(LinearLayout page) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        addFieldLabel(card, "Email Address");
        EditText emailInput = Ui.input(this, "healhmate@sample.com", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        card.addView(emailInput, fullWidthHeight(48));
        addFieldLabel(card, "Password", 14);
        EditText passwordInput = Ui.input(this, "8 characters must", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        card.addView(passwordInput, fullWidthHeight(48));

        LinearLayout options = Ui.horizontal(this);
        CheckBox remember = new CheckBox(this);
        remember.setText("Remember me");
        remember.setTextColor(Ui.MUTED);
        remember.setTextSize(13);
        remember.setChecked(true);
        options.addView(remember, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        TextView forgot = Ui.text(this, "Forgot Password?", 13, Ui.DARK_GREEN, true);
        forgot.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        options.addView(forgot);
        forgot.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Password help")
                .setMessage("Wait and See")
                .setPositiveButton("OK", null)
                .show());
        card.addView(options, fullWidthHeight(42));

        TextView submit = Ui.button(this, "Sign In", Ui.GREEN, Color.WHITE, 13, true);
        card.addView(submit, Ui.margins(this, -1, 48, 0, 7, 0, 3));
        submit.setOnClickListener(view -> signIn(emailInput, passwordInput, remember.isChecked()));

        LinearLayout footer = Ui.horizontal(this);
        footer.setGravity(Gravity.CENTER);
        TextView prompt = Ui.text(this, "Don't have an account?  ", 13, Ui.MUTED, false);
        TextView signup = Ui.text(this, "Sign Up", 13, Ui.DARK_GREEN, true);
        footer.addView(prompt);
        footer.addView(signup);
        card.addView(footer, fullWidthHeight(38));
        signup.setOnClickListener(view -> {
            activeAuthTab = "register";
            showAuth();
        });
        page.addView(card, Ui.margins(this, -1, -2, 0, 0, 0, 15));

    }

    private void buildRegisterForm(LinearLayout page) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 17), Ui.dp(this, 18), Ui.dp(this, 16));

        addFieldLabel(card, "Full Name");
        EditText nameInput = Ui.input(this, "Your Full name", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        card.addView(nameInput, fullWidthHeight(46));
        addFieldLabel(card, "Email Address", 12);
        EditText emailInput = Ui.input(this, "healthmate@sample.com", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        card.addView(emailInput, fullWidthHeight(46));
        addFieldLabel(card, "Password", 12);
        EditText passwordInput = Ui.input(this, "8 characters must", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(passwordInput, fullWidthHeight(46));
        addFieldLabel(card, "Confirm Password", 12);
        EditText confirmInput = Ui.input(this, "Enter your password again", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(confirmInput, fullWidthHeight(46));

        CheckBox terms = new CheckBox(this);
        terms.setText("I agree to the Terms & Privacy Policy");
        terms.setTextSize(13);
        terms.setTextColor(Ui.MUTED);
        terms.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        terms.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 5));
        card.addView(terms, fullWidthHeight(43));

        TextView submit = Ui.button(this, "Create Account", Ui.GREEN, Color.WHITE, 13, true);
        card.addView(submit, Ui.margins(this, -1, 48, 0, 4, 0, 4));
        submit.setOnClickListener(view -> register(nameInput, emailInput, passwordInput, confirmInput, terms.isChecked()));

        LinearLayout footer = Ui.horizontal(this);
        footer.setGravity(Gravity.CENTER);
        footer.addView(Ui.text(this, "Already have an account?  ", 13, Ui.MUTED, false));
        TextView signin = Ui.text(this, "Sign In", 13, Ui.DARK_GREEN, true);
        footer.addView(signin);
        card.addView(footer, fullWidthHeight(36));
        signin.setOnClickListener(view -> {
            activeAuthTab = "signin";
            showAuth();
        });
        page.addView(card, Ui.margins(this, -1, -2, 0, 0, 0, 14));
    }

    private void addFieldLabel(LinearLayout parent, String label) {
        addFieldLabel(parent, label, 14);
    }

    private void addFieldLabel(LinearLayout parent, String label, int topMargin) {
        TextView view = Ui.text(this, label, 13, Ui.TEXT, true);
        parent.addView(view, Ui.margins(this, -1, 22, 0, topMargin, 0, 6));
    }

    private void signIn(EditText emailInput, EditText passwordInput, boolean remember) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (!isValidEmail(email) || password.length() < 8) {
            showMessage("Enter a valid email or password of at least 8 characters.");
            return;
        }
        try {
            User user = authController.login(email, password.toCharArray(), remember);
            if (user == null) {
                showMessage("Email or password is incorrect.");
                return;
            }
            currentUser = user;
            activeTab = "today";
            renderApp();
        } catch (Exception error) {
            showMessage("Can't find your account. Please try again.");
        }
    }

    private void register(EditText nameInput, EditText emailInput, EditText passwordInput, EditText confirmInput, boolean acceptedTerms) {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirm = confirmInput.getText().toString();
        if (name.length() < 2) {
            showMessage("Enter your full name.");
            return;
        }
        if (!isValidEmail(email)) {
            showMessage("Enter a valid email address.");
            return;
        }
        if (password.length() < 8) {
            showMessage("Use a password with at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showMessage("The passwords do not match.");
            return;
        }
        if (!acceptedTerms) {
            showMessage("Please accept the terms to create an account.");
            return;
        }
        try {
            currentUser = authController.register(name, email, password.toCharArray());
            showMessage("Registered successfully");
            activeTab = "today";
            renderApp();
        } catch (IllegalArgumentException duplicate) {
            showMessage(duplicate.getMessage());
        } catch (Exception error) {
            showMessage("Your account could not be saved. Please try again.");
        }
    }
    private boolean isValidEmail(String value) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }

    private void renderApp() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BACKGROUND);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header = Ui.horizontal(this);
        header.setPadding(Ui.dp(this, 28), Ui.dp(this, 8), Ui.dp(this, 28), Ui.dp(this, 8));
        addBrandMark(header, false);
        LinearLayout brandText = Ui.vertical(this);
        TextView brand = Ui.text(this, " HealthMate", 15, Ui.TEXT, true);
        TextView section = Ui.text(this, tabTitle(activeTab), 11, Ui.MUTED, false);
        brandText.addView(brand);
        brandText.addView(section);
        header.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView profile = Ui.text(this, "\uD81A\uDE06", 18, Color.WHITE, true);
        profile.setGravity(Gravity.CENTER);
        profile.setBackground(Ui.rounded(Ui.DARK_GREEN, Ui.dp(this, 22), Color.TRANSPARENT, 0));
        header.addView(profile, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));
        profile.setOnClickListener(view -> showProfileDialog());
        root.addView(header, fullWidthHeight(56));

        ScrollView content = new ScrollView(this);
        content.setFillViewport(true);
        content.setClipToPadding(false);
        LinearLayout page = Ui.vertical(this);
        page.setPadding(Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 14), Ui.dp(this, 18));
        content.addView(page);
        if ("log".equals(activeTab)) {
            buildLogPage(page);
        } else if ("history".equals(activeTab)) {
            buildHistoryPage(page);
        } else if ("trends".equals(activeTab)) {
            buildTrendsPage(page);
        } else {
            buildTodayPage(page);
        }
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 64));
        navParams.setMargins(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 8));
        root.addView(buildBottomNavigation(), navParams);
        setContentView(root);
    }

    private String tabTitle(String tab) {
        if ("log".equals(tab)) return "Log";
        if ("history".equals(tab)) return "History";
        if ("trends".equals(tab)) return "Trends";
        return "Today";
    }

    private void buildTodayPage(LinearLayout page) {
        HealthRecord today = getRecord(selectedDate);
        int currentSteps = today == null ? 0 : today.getSteps();
        int currentCalories = today == null ? 0 : today.getCalories();
        int currentWater = today == null ? 0 : today.getWaterMl();

        LinearLayout welcome = Ui.card(this);
        welcome.setBackground(Ui.rounded(Ui.PALE_BLUE, Ui.dp(this, 18), Color.TRANSPARENT, 0));
        addLabel(welcome, " Today – " + selectedDate.format(DISPLAY_DATE), 12, Ui.MUTED, true);
        addLabel(welcome, "Hello, " + firstName(currentUser.getName()) + "!", 21, Ui.TEXT, true);
        addLabel(welcome, "Here is your daily wellness summary.", 13, Ui.MUTED, false);
        addCard(page, welcome, 0, 12);

        LinearLayout stepCard = buildSummaryCard(
                "\uD83D\uDC63", "Steps", "Goal: 10,000 steps",
                formatNumber(currentSteps) + " steps",
                currentSteps, STEP_GOAL,
                currentSteps >= STEP_GOAL
                        ? "Goal met"
                        : formatNumber(Math.max(0, STEP_GOAL - currentSteps)) + " remaining",
                Color.rgb(237, 255, 239), Color.rgb(0, 120, 16) );
        addCard(page, stepCard, 0, 12);

        LinearLayout calorieCard = buildSummaryCard(
                "\uD83D\uDD25", "Calories Burned", "Target: 600 kcal",
                formatNumber(currentCalories) + " kcal",
                currentCalories, CALORIE_GOAL,
                currentCalories >= CALORIE_GOAL
                        ? "Target met"
                        : formatNumber(Math.max(0, CALORIE_GOAL - currentCalories)) + " kcal left",
                Color.rgb(255, 242, 220), Color.rgb(151, 94, 16) );
        addCard(page, calorieCard, 0, 12);

        LinearLayout waterCard = buildSummaryCard(
                "\uD83D\uDCA7", "Water Intake", "Target: 2,500 ml",
                formatNumber(currentWater) + " ml",
                currentWater, WATER_GOAL,
                currentWater >= WATER_GOAL
                        ? "Goal met"
                        : formatNumber(Math.max(0, WATER_GOAL - currentWater)) + " ml remaining",
                Color.rgb(220, 242, 252), Color.rgb(20, 122, 164) );
        addCard(page, waterCard, 0, 12);

        TextView addEntry = Ui.button(this, "Add Daily Entry  ---->", Ui.GREEN, Color.WHITE, 12, true);
        addEntry.setTextSize(15);
        page.addView(addEntry, Ui.margins(this, -1, 48, 0, 5, 0, 6));
        addEntry.setOnClickListener(view -> openLogForDate(LocalDate.now(), false));

        if (today == null) {
            addLabel(page, "Let's Bang.", 12, Ui.MUTED, false);
        }
    }

    private LinearLayout buildSummaryCard(String icon, String title, String target, String value, int progress, int goal, String suffix, int iconBackground, int iconColor) {
        LinearLayout card = Ui.card(this);
        LinearLayout titleRow = Ui.horizontal(this);
        titleRow.addView(iconBubble(icon, iconBackground, iconColor), new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));
        LinearLayout labels = Ui.vertical(this);
        labels.addView(Ui.text(this, title, 14, Ui.TEXT, true));
        labels.addView(Ui.text(this, target, 11, Ui.MUTED, false));
        titleRow.addView(labels, Ui.margins(this, -2, -2, 10, 0, 0, 0));
        titleRow.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        TextView badge = Ui.button(this, formatNumber(progress) + " / " + formatNumber(goal), Ui.PALE_GREEN, Ui.DARK_GREEN, 12, true);
        badge.setTextSize(10);
        titleRow.addView(badge);
        card.addView(titleRow);
        addLabel(card, value, 22, Ui.TEXT, true);
        addLabel(card, suffix, 11, Ui.MUTED, false);
        card.addView(progressBar(progress, goal), Ui.margins(this, -1, 8, 0, 7, 0, 0));
        return card;
    }

    private TextView iconBubble(String value, int background, int foreground) {
        TextView icon = Ui.text(this, value, 16, foreground, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.rounded(background, Ui.dp(this, 20), Color.TRANSPARENT, 0));
        return icon;
    }

    private ProgressBar progressBar(int value, int maximum) {
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, maximum));
        bar.setProgress(Math.min(value, maximum));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(223, 232, 248)));
        bar.setMinimumHeight(Ui.dp(this, 8));
        return bar;
    }

    private void addWater(int amount) {
        HealthRecord existing = getRecord(LocalDate.now());
        int nextWater = (existing == null ? 0 : existing.getWaterMl()) + amount;
        if (nextWater > 10_000) {
            showMessage("Daily water intake cannot exceed 10,000 ml.");
            return;
        }
        boolean saved = saveRecord(new HealthRecord(LocalDate.now().format(ISO_DATE),
                existing == null ? 0 : existing.getSteps(),
                existing == null ? 0 : existing.getCalories(),
                nextWater, existing == null ? "" : existing.getNote()));
        if (!saved) return;
        selectedDate = LocalDate.now();
        renderApp();
    }

    private void buildLogPage(LinearLayout page) {
        addLabel(page, "\uD83D\uDCCB " + (editingDate == null ? "Add Daily Record" : "Edit Daily Record"), 20, Ui.TEXT, true);
        addLabel(page, "Log your daily physical activities and hydration metrics.", 12, Ui.MUTED, false);

        LinearLayout dateCard = Ui.card(this);
        addLabel(dateCard, "Date", 13, Ui.TEXT, true);
        LinearLayout dateRow = Ui.horizontal(this);
        dateRow.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 10), 0);
        dateRow.setBackground(Ui.rounded(Ui.PALE_BLUE, Ui.dp(this, 13), Color.TRANSPARENT, 0));
        TextView dateText = Ui.text(this, "●  " + selectedDate.format(DISPLAY_DATE), 15, Ui.TEXT, true);
        dateRow.addView(dateText, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));
        TextView todayButton = Ui.button(this, "Today", Color.TRANSPARENT, Ui.MUTED, 10, true);
        dateRow.addView(todayButton);
        dateCard.addView(dateRow);
        dateRow.setOnClickListener(view -> chooseDate());
        dateText.setOnClickListener(view -> chooseDate());
        todayButton.setOnClickListener(view -> {
            selectedDate = LocalDate.now();
            loadRecordForForm(selectedDate);
            renderApp();
        });
        addCard(page, dateCard, 10, 12);

        page.addView(buildCounterCard("\uD83D\uDC63  Steps Walked", "Daily target: 10,000 steps", "steps", steps, 500, new String[]{"+500", "+1,000"},
                new int[]{500, 1000}, value -> steps = value), Ui.margins(this, -1, -2, 0, 0, 0, 12));
        page.addView(buildCounterCard("\uD83D\uDD25  Calories Burned", "Active & exercise metrics", "kcal", calories, 50, new String[]{"+50", "+150", "+300"},
                new int[]{50, 150, 300}, value -> calories = value), Ui.margins(this, -1, -2, 0, 0, 0, 12));
        page.addView(buildCounterCard("\uD83D\uDCA7  Water Intake", "Recommended: 2,500 ml", "ml", waterMl, 250, new String[]{"250 ml", "500 ml", "750 ml"},
                new int[]{250, 500, 750}, value -> waterMl = value), Ui.margins(this, -1, -2, 0, 0, 0, 12));

        LinearLayout noteCard = Ui.card(this);
        addLabel(noteCard, "Daily note (optional)", 13, Ui.TEXT, true);
        noteInput = Ui.input(this, "e.g. trial was good", android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        HealthRecord savedForDate = getRecord(selectedDate);
        if (savedForDate != null) {
            noteInput.setText(savedForDate.getNote());
        }
        noteCard.addView(noteInput, fullWidthHeight(46));
        addCard(page, noteCard, 0, 10);

        formStatus = Ui.text(this, "✓  Form complete: all values are within valid ranges.",
                12, Ui.DARK_GREEN, false);
        formStatus.setPadding(Ui.dp(this, 12), Ui.dp(this, 11), Ui.dp(this, 12), Ui.dp(this, 11));
        formStatus.setBackground(Ui.rounded(Ui.PALE_GREEN, Ui.dp(this, 13), Color.TRANSPARENT, 0));
        page.addView(formStatus, Ui.margins(this, -1, -2, 0, 0, 0, 9));

        TextView save = Ui.button(this, editingDate == null ? "✓  Save Record" : "✓  Update Record", Ui.GREEN, Color.WHITE, 12, true);
        save.setTextSize(15);
        page.addView(save, Ui.margins(this, -1, 48, 0, 0, 0, 8));
        save.setOnClickListener(view -> saveForm());
        TextView reset = Ui.button(this, "↻  Reset Form", Ui.PALE_BLUE, Ui.TEXT, 12, true);
        reset.setTextSize(14);
        page.addView(reset, fullWidthHeight(44));
        reset.setOnClickListener(view -> resetForm());
    }

    private LinearLayout buildCounterCard(String title, String subtitle, String unit, int startingValue, int increment, String[] quickLabels, int[] quickValues, IntConsumer onChange) {
        LinearLayout card = Ui.card(this);
        LinearLayout heading = Ui.horizontal(this);
        LinearLayout textColumn = Ui.vertical(this);
        textColumn.addView(Ui.text(this, title, 15, Ui.TEXT, true));
        textColumn.addView(Ui.text(this, subtitle, 11, Ui.MUTED, false));
        heading.addView(textColumn, new LinearLayout.LayoutParams(0, -2, 1));
        TextView unitBadge = Ui.button(this, unit, Ui.PALE_GREEN, Ui.DARK_GREEN, 10, true);
        unitBadge.setTextSize(11);
        heading.addView(unitBadge);
        card.addView(heading);

        LinearLayout numberRow = Ui.horizontal(this);
        numberRow.setPadding(Ui.dp(this, 5), Ui.dp(this, 4), Ui.dp(this, 5), Ui.dp(this, 4));
        numberRow.setBackground(Ui.rounded(Ui.PALE_BLUE, Ui.dp(this, 15), Color.TRANSPARENT, 0));
        TextView minus = Ui.button(this, "−", Ui.WHITE, Ui.TEXT, 12, true);
        TextView value = Ui.text(this, formatNumber(startingValue), 23, Ui.TEXT, true);
        value.setGravity(Gravity.CENTER);
        TextView unitLabel = Ui.text(this, unit, 11, Ui.MUTED, false);
        unitLabel.setGravity(Gravity.CENTER);
        TextView plus = Ui.button(this, "+", Ui.WHITE, Ui.TEXT, 12, true);
        numberRow.addView(minus, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 38)));
        numberRow.addView(value, new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1));
        numberRow.addView(unitLabel);
        numberRow.addView(plus, Ui.margins(this, 40, 38, 8, 0, 0, 0));
        card.addView(numberRow, Ui.margins(this, -1, 46, 0, 12, 0, 0));

        minus.setOnClickListener(view -> {
            int next = Math.max(0, currentMetric(value) - increment);
            value.setText(formatNumber(next));
            onChange.accept(next);
            updateFormStatus();
        });
        plus.setOnClickListener(view -> {
            int limit = "steps".equals(unit) ? 100_000 : 10_000;
            int next = Math.min(limit, currentMetric(value) + increment);
            value.setText(formatNumber(next));
            onChange.accept(next);
            updateFormStatus();
        });

        LinearLayout quickRow = Ui.horizontal(this);
        TextView quickTitle = Ui.text(this, "Quick:", 11, Ui.MUTED, true);
        quickRow.addView(quickTitle, Ui.margins(this, 42, 34, 0, 0, 6, 0));
        for (int i = 0; i < quickLabels.length; i++) {
            final int quickValue = quickValues[i];
            TextView quick = Ui.button(this, quickLabels[i], Ui.PALE_BLUE, Ui.TEXT, 10, true);
            quick.setTextSize(10);
            LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1);
            quickParams.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            quickRow.addView(quick, quickParams);
            quick.setOnClickListener(view -> {
                int limit = "steps".equals(unit) ? 100_000 : 10_000;
                int next = Math.min(limit, currentMetric(value) + quickValue);
                value.setText(formatNumber(next));
                onChange.accept(next);
                updateFormStatus();
            });
        }
        card.addView(quickRow, Ui.margins(this, -1, 34, 0, 7, 0, 0));
        return card;
    }

    private int currentMetric(TextView view) {
        try {
            return Integer.parseInt(view.getText().toString().replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void chooseDate() {
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = LocalDate.of(year, month + 1, day);
            loadRecordForForm(selectedDate);
            renderApp();
        }, selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void loadRecordForForm(LocalDate date) {
        HealthRecord record = getRecord(date);
        if (record == null) {
            steps = 0;
            calories = 0;
            waterMl = 0;
            editingDate = null;
        } else {
            steps = record.getSteps();
            calories = record.getCalories();
            waterMl = record.getWaterMl();
            editingDate = record.getDate();
        }
    }

    private void saveForm() {
        String validationError = healthController.validate(steps, calories, waterMl);
        if (validationError != null) {
            showMessage(validationError);
            updateFormStatus();
            return;
        }
        String note = noteInput == null ? "" : noteInput.getText().toString();
        if (!saveRecord(new HealthRecord(selectedDate.format(ISO_DATE), steps, calories, waterMl, note))) {
            return;
        }
        editingDate = null;
        activeTab = "today";
        selectedDate = LocalDate.now();
        showMessage("Daily record saved.");
        renderApp();
    }

    private void resetForm() {
        steps = 0;
        calories = 0;
        waterMl = 0;
        selectedDate = LocalDate.now();
        editingDate = null;
        renderApp();
        if (noteInput != null) {
            noteInput.setText("");
        }
    }

    private void updateFormStatus() {
        if (formStatus == null) {
            return;
        }
        boolean valid = healthController.validate(steps, calories, waterMl) == null;
        formStatus.setText(valid
                ? "✓  Form complete: all values are within valid ranges."
                : "Check the values before saving this record.");
        formStatus.setTextColor(valid ? Ui.DARK_GREEN : Ui.RED);
        formStatus.setBackground(Ui.rounded(valid ? Ui.PALE_GREEN : Color.rgb(255, 235, 235),
                Ui.dp(this, 13), Color.TRANSPARENT, 0));
    }

    private void buildHistoryPage(LinearLayout page) {
        historySearch = Ui.input(this, "Search by date (e.g. 17 Aug)...", android.text.InputType.TYPE_CLASS_TEXT);
        historySearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        historySearch.setText(historyQuery);
        page.addView(historySearch, Ui.margins(this, -1, 48, 0, 0, 0, 9));

        LinearLayout controls = Ui.horizontal(this);
        TextView sort = Ui.button(this, oldestFirst ? "↕  Oldest First  ⌄" : "↕  Newest First  ⌄", Ui.WHITE, Ui.TEXT, 11, true);
        sort.setBackground(Ui.rounded(Ui.WHITE, Ui.dp(this, 12), Ui.BORDER, Ui.dp(this, 1)));
        controls.addView(sort);
        TextView spacer = Ui.text(this, "", 1, Ui.MUTED, false);
        controls.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView all = Ui.button(this, "All", "all".equals(historyRange) ? Ui.PALE_BLUE : Ui.WHITE, "all".equals(historyRange) ? Ui.TEXT : Ui.MUTED, 14, true);
        TextView recent = Ui.button(this, "✓  Past 7 Days", "7days".equals(historyRange) ? Ui.GREEN : Ui.WHITE, "7days".equals(historyRange) ? Color.WHITE : Ui.MUTED, 14, true);
        controls.addView(all, Ui.margins(this, -2, 34, 5, 0, 0, 0));
        controls.addView(recent, Ui.margins(this, -2, 34, 5, 0, 0, 0));
        page.addView(controls, Ui.margins(this, -1, 38, 0, 0, 0, 10));

        historyResults = Ui.vertical(this);
        page.addView(historyResults, fullWidthHeight(-2));
        TextView count = Ui.text(this, "", 11, Ui.MUTED, true);
        count.setGravity(Gravity.CENTER);
        count.setBackground(Ui.rounded(Ui.PALE_BLUE, Ui.dp(this, 13), Color.TRANSPARENT, 0));
        page.addView(count, Ui.margins(this, -1, 38, 0, 8, 0, 0));

        sort.setOnClickListener(view -> {
            oldestFirst = !oldestFirst;
            populateHistoryList(count);
            sort.setText(oldestFirst ? "↕  Oldest First  ⮟" : "↕  Newest First  ⮟");
        });
        all.setOnClickListener(view -> {
            historyRange = "all";
            renderApp();
        });
        recent.setOnClickListener(view -> {
            historyRange = "7days";
            renderApp();
        });
        historySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int countBefore, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int countAfter) {
                historyQuery = value.toString();
                populateHistoryList(count);
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        populateHistoryList(count);
    }

    private void populateHistoryList(TextView countLabel) {
        if (historyResults == null) {
            return;
        }
        historyResults.removeAllViews();
        List<HealthRecord> records;
        try {
            records = healthController.getHealthRecords(currentUser.getEmail());
        } catch (Exception error) {
            addLabel(historyResults, "Could not load saved records.", 13, Ui.RED, false);
            return;
        }
        // Search + 7-day filter + sorting are handled by HistoryController
        String query = historyQuery.trim().toLowerCase(Locale.ROOT);
        List<HealthRecord> filtered =
                historyController.filterHistory(records, historyQuery, historyRange, oldestFirst);

        if (filtered.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            addLabel(empty, "No health records found", 15, Ui.TEXT, true);
            addLabel(empty, query.isEmpty() ? "Add a daily entry to start building your history." : "Try another date or clear the search.", 12, Ui.MUTED, false);
            historyResults.addView(empty, Ui.margins(this, -1, -2, 0, 1, 0, 9));
        } else {
            for (HealthRecord record : filtered) {
                historyResults.addView(buildHistoryCard(record), Ui.margins(this, -1, -2, 0, 0, 0, 9));
            }
        }
        countLabel.setText("●  Showing " + filtered.size() + " record" + (filtered.size() == 1 ? "" : "s"));
    }

    private LinearLayout buildHistoryCard(HealthRecord record) {
        LinearLayout card = Ui.card(this);
        LocalDate date = parseDate(record.getDate());
        String title = date == null ? record.getDate()
                : date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        int completion = Math.round((goalPercent(record.getSteps(), STEP_GOAL)
                + goalPercent(record.getCalories(), CALORIE_GOAL)
                + goalPercent(record.getWaterMl(), WATER_GOAL)) / 3f);

        LinearLayout heading = Ui.horizontal(this);
        TextView dateTitle = Ui.text(this, title, 16, Ui.TEXT, true);
        heading.addView(dateTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = Ui.button(this, completion >= 100 ? "✓  Goal Met" : "Partial (" + completion + "%)",
                completion >= 100 ? Ui.PALE_GREEN : Ui.PALE_BLUE,
                completion >= 100 ? Ui.DARK_GREEN : Ui.MUTED, 11, true);
        heading.addView(badge);
        card.addView(heading);

        LinearLayout metrics = Ui.horizontal(this);
        metrics.setPadding(Ui.dp(this, 8), Ui.dp(this, 9), Ui.dp(this, 8), Ui.dp(this, 9));
        metrics.setBackground(Ui.rounded(Ui.PALE_BLUE, Ui.dp(this, 12), Color.TRANSPARENT, 0));
        addHistoryMetric(metrics, "\uD83D\uDC63 Steps", formatNumber(record.getSteps()), "", 0);
        addHistoryMetric(metrics, "\uD83D\uDD25 Calories", formatNumber(record.getCalories()), "kcal", 1);
        addHistoryMetric(metrics, "\uD83D\uDCA7 Water", formatNumber(record.getWaterMl()), "ml", 2);
        card.addView(metrics, Ui.margins(this, -1, -2, 0, 10, 0, 0));
        card.addView(progressBar(completion, 100), Ui.margins(this, -1, 7, 0, 7, 0, 4));

        LinearLayout noteRow = Ui.horizontal(this);
        String note = record.getNote().isEmpty() ? "▣  Daily Log" : "\uD83D\uDCAC  " + record.getNote();
        TextView noteText = Ui.text(this, note, 12, Ui.MUTED, false);
        noteRow.addView(noteText, new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1));
        TextView edit = Ui.button(this, "\uD83D\uDCDD  Edit", Ui.PALE_BLUE, Ui.DARK_GREEN, 12, true);
        TextView delete = Ui.button(this, "\uD83D\uDDD1\uFE0F  Delete", Color.rgb(255, 226, 226), Ui.RED, 12, true);
        noteRow.addView(edit, Ui.margins(this, -2, 38, 5, 0, 0, 0));
        noteRow.addView(delete, Ui.margins(this, -2, 38, 5, 0, 0, 0));
        card.addView(noteRow, Ui.margins(this, -1, -2, 0, 2, 0, 0));
        edit.setOnClickListener(view -> openLogForDate(date == null ? LocalDate.now() : date, true));
        delete.setOnClickListener(view -> confirmDelete(record));
        return card;
    }

    private void addHistoryMetric(LinearLayout parent, String label, String value, String unit, int index) {
        LinearLayout column = Ui.vertical(this);
        TextView labelView = Ui.text(this, label, 10, Ui.MUTED, false);
        TextView valueView = Ui.text(this, value + (unit.isEmpty() ? "" : " " + unit), 13, Ui.TEXT, true);
        column.addView(labelView);
        column.addView(valueView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        if (index > 0) {
            params.leftMargin = Ui.dp(this, 4);
        }
        parent.addView(column, params);
    }

    private int goalPercent(int value, int goal) {
        return Math.min(100, Math.round(value * 100f / goal));
    }

    private void confirmDelete(HealthRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this record?")
                .setMessage("The entry for " + record.getDate() + " will be removed from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        healthController.deleteHealthRecord(currentUser.getEmail(), record.getDate());
                        showMessage("Record deleted.");
                        renderApp();
                    } catch (Exception error) {
                        showMessage("The record could not be deleted.");
                    }
                })
                .show();
    }
    private void buildTrendsPage(LinearLayout page) {
        LinearLayout periodRow = Ui.horizontal(this);
        LocalDate today = LocalDate.now();
        LocalDate start = monthlyTrends ? today.minusDays(27) : today.minusDays(6);
        TextView dateRange = Ui.text(this, "▣  " + start.format(DateTimeFormatter.ofPattern("dd MMM")) + " – " + today.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), 11, Ui.MUTED, true);
        periodRow.addView(dateRange, new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1));
        TextView week = Ui.button(this, "Week", monthlyTrends ? Ui.WHITE : Ui.PALE_GREEN, monthlyTrends ? Ui.MUTED : Ui.DARK_GREEN, 11, true);
        TextView month = Ui.button(this, "Month", monthlyTrends ? Ui.PALE_GREEN : Ui.WHITE, monthlyTrends ? Ui.DARK_GREEN : Ui.MUTED, 11, true);
        periodRow.addView(week);
        periodRow.addView(month);
        page.addView(periodRow, Ui.margins(this, -1, 40, 0, 0, 0, 3));
        week.setOnClickListener(view -> {
            monthlyTrends = false;
            renderApp();
        });
        month.setOnClickListener(view -> {
            monthlyTrends = true;
            renderApp();
        });

        List<HealthRecord> records = getRecords();
        List<HealthRecord> periodRecords = recordsInRange(records, monthlyTrends ? today.minusDays(27) : today.minusDays(6), today);
        long daysLogged = periodRecords.size();
        int averageSteps = average(periodRecords, 0);
        int totalCalories = sum(periodRecords, 1);
        int averageWater = average(periodRecords, 2);
        long hydrationDays = periodRecords.stream().filter(record -> record.getWaterMl() >= WATER_GOAL).count();

        LinearLayout performance = Ui.card(this);
        addLabel(performance, monthlyTrends ? "Monthly Performance" : "Weekly Performance", 17, Ui.TEXT, true);
        addLabel(performance, "Your daily activity compared with an 8,000-step target.", 11, Ui.MUTED, false);
        TrendChartView chart = new TrendChartView(this);
        ArrayList<Integer> chartValues = new ArrayList<>();
        ArrayList<String> chartLabels = new ArrayList<>();
        fillChart(records, today, chartValues, chartLabels);
        chart.setChartData(chartValues, chartLabels, 8_000);
        performance.addView(chart, Ui.margins(this, -1, 190, 0, 4, 0, 0));
        addCard(page, performance, 0, 10);

        LinearLayout summary = Ui.card(this);
        addLabel(summary, "\uD83D\uDC63  Daily Average Steps", 12, Ui.MUTED, false);
        LinearLayout stepSummary = Ui.horizontal(this);
        stepSummary.addView(Ui.text(this, formatNumber(averageSteps), 25, Ui.TEXT, true));
        stepSummary.addView(Ui.text(this, "  steps/day", 12, Ui.MUTED, false));
        stepSummary.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        TextView loggedBadge = Ui.button(this, daysLogged + " logged", Ui.PALE_GREEN, Ui.DARK_GREEN, 11, true);
        stepSummary.addView(loggedBadge);
        summary.addView(stepSummary);
        addCard(page, summary, 0, 10);

        LinearLayout pair = Ui.horizontal(this);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -2, 1f);
        leftParams.setMargins(0, 0, Ui.dp(this, 5), 0);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -2, 1f);
        rightParams.setMargins(Ui.dp(this, 5), 0, 0, 0);
        pair.addView(buildSmallStat("Total Cal Burn", formatNumber(totalCalories), "kcal", periodRecords.isEmpty() ? "No entries yet" : "Across logged days", Color.rgb(255, 242, 220), "\uD83D\uDD25"), leftParams);
        pair.addView(buildSmallStat("Hydration Rate", formatNumber(averageWater), "ml / day", hydrationDays + " goal days", Color.rgb(220, 242, 252), "\uD83D\uDCA7"), rightParams);
        page.addView(pair, Ui.margins(this, -1, -2, 0, 0, 0, 10));

        LinearLayout milestones = Ui.card(this);
        addLabel(milestones, "Weekly Milestones", 15, Ui.TEXT, true);
        addMilestone(milestones, "Hydration streak",
                currentHydrationStreak(records) + " day" + (currentHydrationStreak(records) == 1 ? "" : "s"),
                "Logged at least 2,500 ml", Ui.PALE_BLUE);
        HealthRecord best = bestSteps(records);
        addMilestone(milestones, "Step champion",
                best == null ? "Not yet" : formatNumber(best.getSteps()) + " steps",
                best == null ? "Log a record to get started" : best.getDate(), Ui.PALE_GREEN);
        addMilestone(milestones, "Consistent burn",
                countBurnDays(periodRecords) + " goal days",
                "At least 400 kcal recorded", Color.rgb(255, 242, 220));
        addCard(page, milestones, 0, 10);
        if (periodRecords.isEmpty()) {
            addLabel(page, "Your trends will appear here after you save a daily record.",
                    12, Ui.MUTED, false);
        }
    }

    private LinearLayout buildSmallStat(String title, String value, String unit,
                                        String detail, int iconColor, String icon) {
        LinearLayout card = Ui.card(this);
        TextView badge = iconBubble(icon, iconColor, Ui.TEXT);
        card.addView(badge, new LinearLayout.LayoutParams(Ui.dp(this, 31), Ui.dp(this, 31)));
        addLabel(card, title, 11, Ui.MUTED, false);
        LinearLayout valueRow = Ui.horizontal(this);
        valueRow.addView(Ui.text(this, value, 21, Ui.TEXT, true));
        valueRow.addView(Ui.text(this, " " + unit, 10, Ui.MUTED, false));
        card.addView(valueRow);
        addLabel(card, detail, 10, Ui.MUTED, false);
        return card;
    }

    private void addMilestone(LinearLayout parent, String title, String value,
                              String detail, int badgeColor) {
        LinearLayout row = Ui.horizontal(this);
        TextView badge = iconBubble("✓", badgeColor, Ui.DARK_GREEN);
        row.addView(badge, new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));
        LinearLayout labels = Ui.vertical(this);
        labels.addView(Ui.text(this, title, 12, Ui.TEXT, true));
        labels.addView(Ui.text(this, detail, 10, Ui.MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(Ui.text(this, value, 10, Ui.DARK_GREEN, true));
        parent.addView(row, Ui.margins(this, -1, 44, 0, 8, 0, 0));
    }

    private void fillChart(List<HealthRecord> records, LocalDate today,
                           List<Integer> values, List<String> labels) {
        Map<String, HealthRecord> byDate = new HashMap<>();
        for (HealthRecord record : records) {
            byDate.put(record.getDate(), record);
        }
        if (monthlyTrends) {
            LocalDate start = today.minusDays(27);
            int[] totals = new int[4];
            int[] counts = new int[4];
            for (HealthRecord record : records) {
                LocalDate date = parseDate(record.getDate());
                if (date != null && !date.isBefore(start) && !date.isAfter(today)) {
                    int bucket = Math.min(3, (int) (ChronoUnit.DAYS.between(start, date) / 7));
                    totals[bucket] += record.getSteps();
                    counts[bucket]++;
                }
            }
            for (int i = 0; i < 4; i++) {
                values.add(counts[i] == 0 ? 0 : totals[i] / counts[i]);
                labels.add("W" + (i + 1));
            }
        } else {
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                HealthRecord record = byDate.get(date.format(ISO_DATE));
                values.add(record == null ? 0 : record.getSteps());
                labels.add(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH));
            }
        }
    }

    private List<HealthRecord> recordsInRange(List<HealthRecord> records, LocalDate start, LocalDate end) {
        List<HealthRecord> result = new ArrayList<>();
        for (HealthRecord record : records) {
            LocalDate date = parseDate(record.getDate());
            if (date != null && !date.isBefore(start) && !date.isAfter(end)) {
                result.add(record);
            }
        }
        return result;
    }

    private int average(List<HealthRecord> records, int field) {
        if (records.isEmpty()) return 0;
        return sum(records, field) / records.size();
    }

    private int sum(List<HealthRecord> records, int field) {
        int total = 0;
        for (HealthRecord record : records) {
            if (field == 0) total += record.getSteps();
            if (field == 1) total += record.getCalories();
            if (field == 2) total += record.getWaterMl();
        }
        return total;
    }

    private int currentHydrationStreak(List<HealthRecord> records) {
        Map<String, HealthRecord> byDate = new HashMap<>();
        for (HealthRecord record : records) byDate.put(record.getDate(), record);
        int streak = 0;
        LocalDate day = LocalDate.now();
        while (true) {
            HealthRecord record = byDate.get(day.format(ISO_DATE));
            if (record == null || record.getWaterMl() < WATER_GOAL) break;
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    private HealthRecord bestSteps(List<HealthRecord> records) {
        HealthRecord best = null;
        for (HealthRecord record : records) {
            if (best == null || record.getSteps() > best.getSteps()) best = record;
        }
        return best;
    }

    private int countBurnDays(List<HealthRecord> records) {
        int count = 0;
        for (HealthRecord record : records) if (record.getCalories() >= 400) count++;
        return count;
    }

    private void openLogForDate(LocalDate date, boolean edit) {
        selectedDate = date;
        loadRecordForForm(date);
        editingDate = edit ? date.format(ISO_DATE) : null;
        activeTab = "log";
        renderApp();
    }

    private LinearLayout buildBottomNavigation() {
        LinearLayout nav = Ui.horizontal(this);
        nav.setGravity(Gravity.CENTER);
        nav.setBackground(Ui.rounded(Ui.WHITE, Ui.dp(this, 30), Ui.BORDER , Ui.dp(this, 1)));
        addNavItem(nav, "today", "\uD83D\uDDD3\uFE0F", "Today");
        addNavItem(nav, "log", "\uD83D\uDCDD", "Log");
        addNavItem(nav, "history", "⌛", "History");
        addNavItem(nav, "trends", "⚡", "Trends");
        return nav;
    }

    private void addNavItem(LinearLayout parent, String key, String icon, String label) {
        boolean selected = key.equals(activeTab);
        LinearLayout item = Ui.vertical(this);
        item.setGravity(Gravity.CENTER);
        TextView iconView = Ui.text(this, icon, 19, selected ? Ui.GREEN : Ui.MUTED, true);
        iconView.setGravity(Gravity.CENTER);
        TextView labelView = Ui.text(this, label, 10, selected ? Ui.GREEN : Ui.MUTED, selected);
        labelView.setGravity(Gravity.CENTER);
        item.addView(iconView, fullWidthHeight(24));
        item.addView(labelView, fullWidthHeight(17));
        parent.addView(item, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        item.setOnClickListener(view -> {
            if ("log".equals(key) && !"log".equals(activeTab)) {
                openLogForDate(LocalDate.now(), false);
            } else {
                activeTab = key;
                if (!"log".equals(key)) editingDate = null;
                renderApp();
            }
        });
    }

    private void showProfileDialog() {
        new AlertDialog.Builder(this)
                .setTitle(currentUser.getName())
                .setMessage(currentUser.getEmail() + "\n\nYour records are saved only on this device.")
                .setNegativeButton("Close", null)
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    authController.logout();
                    currentUser = null;
                    activeAuthTab = "signin";
                    showAuth();
                })
                .show();
    }


    private boolean saveRecord(HealthRecord record) {
        try {
            healthController.saveHealthRecord(currentUser.getEmail(), record);
            return true;
        } catch (Exception error) {
            showMessage("The record could not be saved to this device.");
            return false;
        }
    }

    private HealthRecord getRecord(LocalDate date) {
        try {
            return healthController.getHealthRecord(currentUser.getEmail(), date.format(ISO_DATE));
        } catch (Exception error) {
            return null;
        }
    }

    private List<HealthRecord> getRecords() {
        try {
            return healthController.getHealthRecords(currentUser.getEmail());
        } catch (Exception error) {
            return new ArrayList<>();
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (Exception error) {
            return null;
        }
    }

    private void addCard(LinearLayout parent, View card, int top, int bottom) {
        parent.addView(card, Ui.margins(this, -1, -2, 0, top, 0, bottom));
    }

    private void addLabel(LinearLayout parent, String value, int size,
                          int color, boolean bold) {
        TextView label = Ui.text(this, value, size, color, bold);
        parent.addView(label, Ui.margins(this, -1, -2, 0, 3, 0, 2));
    }

    private String firstName(String name) {
        if (name == null || name.trim().isEmpty()) return "there";
        return name.trim().split("\\s+")[0];
    }

    private String formatNumber(int number) {
        return String.format(Locale.US, "%,d", number);
    }

    private LinearLayout.LayoutParams fullWidthHeight(int heightDp) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, heightDp));
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleBack() {
        if (currentUser != null && !"today".equals(activeTab)) {
            activeTab = "today";
            editingDate = null;
            renderApp();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }


}
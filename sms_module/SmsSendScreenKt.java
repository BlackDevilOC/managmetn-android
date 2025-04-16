package com.substituemanagment.managment.ui.screens;

import android.content.Context;
import androidx.compose.foundation.ClickableKt;
import androidx.compose.foundation.layout.ColumnScope;
import androidx.compose.foundation.layout.PaddingKt;
import androidx.compose.foundation.layout.SizeKt;
import androidx.compose.foundation.shape.RoundedCornerShapeKt;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.CardKt;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.material3.SurfaceKt;
import androidx.compose.runtime.Composer;
import androidx.compose.runtime.ComposerKt;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.RecomposeScopeImplKt;
import androidx.compose.runtime.ScopeUpdateScope;
import androidx.compose.runtime.State;
import androidx.compose.runtime.internal.ComposableLambdaKt;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.unit.Dp;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.navigation.NavController;
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt__Builders_commonKt;
import kotlinx.coroutines.CoroutineScope;
/* compiled from: SmsSendScreen.kt */
@Metadata(d1 = {"\u00008\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0010\u001a\u0015\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0007¢\u0006\u0002\u0010\u0004\u001a\u0015\u0010\u0005\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u0007H\u0003¢\u0006\u0002\u0010\b\u001am\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u000f2\u0006\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\r2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u000f2\u0006\u0010\u0013\u001a\u00020\u000b2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u000fH\u0003¢\u0006\u0002\u0010\u0018\u001a \u0010\u0019\u001a\u00020\u000b2\u0006\u0010\u001a\u001a\u00020\u000b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u001b\u001a\u00020\u0016H\u0002\u001a\u0016\u0010\u001c\u001a\u00020\u000b2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015H\u0002¨\u0006\u001d²\u0006\n\u0010\u001e\u001a\u00020\rX\u008a\u008e\u0002²\u0006\f\u0010\u001f\u001a\u0004\u0018\u00010\u000bX\u008a\u008e\u0002²\u0006\n\u0010 \u001a\u00020\rX\u008a\u008e\u0002²\u0006\n\u0010!\u001a\u00020\rX\u008a\u0084\u0002²\u0006\f\u0010\"\u001a\u0004\u0018\u00010\u000bX\u008a\u0084\u0002²\u0006\n\u0010#\u001a\u00020\rX\u008a\u0084\u0002²\u0006\n\u0010$\u001a\u00020\rX\u008a\u008e\u0002²\u0006\n\u0010%\u001a\u00020\u000bX\u008a\u008e\u0002²\u0006\n\u0010&\u001a\u00020\rX\u008a\u008e\u0002"}, d2 = {"SmsSendScreen", "", "navController", "Landroidx/navigation/NavController;", "(Landroidx/navigation/NavController;Landroidx/compose/runtime/Composer;I)V", "SmsHistoryItem", "sms", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$SmsMessage;", "(Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$SmsMessage;Landroidx/compose/runtime/Composer;I)V", "ExpandableTeacherItem", "teacherName", "", "isSelected", "", "onSelectionChange", "Lkotlin/Function0;", "assignmentsSummary", "isExpanded", "onExpandToggle", "messageTemplate", "assignments", "", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$AssignmentData;", "onSendIndividual", "(Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Ljava/util/List;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)V", "createPreviewMessage", "template", "assignment", "getAssignmentsSummary", "app_debug", "showHistory", "expandedTeacherId", "showTemplate", "isLoading", "errorMessage", "needsPermission", "showSuccessBar", "successMessage", "showPermissionDialog"}, k = 2, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes9.dex */
public final class SmsSendScreenKt {
    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit ExpandableTeacherItem$lambda$34(String str, boolean z, Function0 function0, String str2, boolean z2, Function0 function02, String str3, List list, Function0 function03, int i, Composer composer, int i2) {
        ExpandableTeacherItem(str, z, function0, str2, z2, function02, str3, list, function03, composer, RecomposeScopeImplKt.updateChangedFlags(i | 1));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit SmsHistoryItem$lambda$31(SmsViewModel.SmsMessage smsMessage, int i, Composer composer, int i2) {
        SmsHistoryItem(smsMessage, composer, RecomposeScopeImplKt.updateChangedFlags(i | 1));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit SmsSendScreen$lambda$30(NavController navController, int i, Composer composer, int i2) {
        SmsSendScreen(navController, composer, RecomposeScopeImplKt.updateChangedFlags(i | 1));
        return Unit.INSTANCE;
    }

    /* JADX WARN: Removed duplicated region for block: B:78:0x03d5  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x03e8  */
    /* JADX WARN: Removed duplicated region for block: B:82:0x0414  */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0427  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x04c9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static final void SmsSendScreen(androidx.navigation.NavController r34, androidx.compose.runtime.Composer r35, final int r36) {
        /*
            Method dump skipped, instructions count: 1257
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.substituemanagment.managment.ui.screens.SmsSendScreenKt.SmsSendScreen(androidx.navigation.NavController, androidx.compose.runtime.Composer, int):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean SmsSendScreen$lambda$1(MutableState<Boolean> mutableState) {
        return mutableState.getValue().booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void SmsSendScreen$lambda$2(MutableState<Boolean> mutableState, boolean z) {
        mutableState.setValue(Boolean.valueOf(z));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final String SmsSendScreen$lambda$4(MutableState<String> mutableState) {
        return mutableState.getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean SmsSendScreen$lambda$7(MutableState<Boolean> mutableState) {
        return mutableState.getValue().booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void SmsSendScreen$lambda$8(MutableState<Boolean> mutableState, boolean z) {
        mutableState.setValue(Boolean.valueOf(z));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final String SmsSendScreen$lambda$12(State<String> state) {
        return (String) state.getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean SmsSendScreen$lambda$14(State<Boolean> state) {
        return ((Boolean) state.getValue()).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean SmsSendScreen$lambda$17(MutableState<Boolean> mutableState) {
        return mutableState.getValue().booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void SmsSendScreen$lambda$18(MutableState<Boolean> mutableState, boolean z) {
        mutableState.setValue(Boolean.valueOf(z));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final String SmsSendScreen$lambda$20(MutableState<String> mutableState) {
        return mutableState.getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean SmsSendScreen$lambda$23(MutableState<Boolean> mutableState) {
        return mutableState.getValue().booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void SmsSendScreen$lambda$24(MutableState<Boolean> mutableState, boolean z) {
        mutableState.setValue(Boolean.valueOf(z));
    }

    private static final void SmsSendScreen$showSuccess(CoroutineScope scope, MutableState<String> mutableState, MutableState<Boolean> mutableState2, String message) {
        mutableState.setValue(message);
        SmsSendScreen$lambda$18(mutableState2, true);
        BuildersKt__Builders_commonKt.launch$default(scope, null, null, new SmsSendScreenKt$SmsSendScreen$showSuccess$1(mutableState2, null), 3, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit SmsSendScreen$lambda$27$lambda$26(SmsViewModel $viewModel, Context $context, final CoroutineScope $scope, final MutableState $successMessage$delegate, final MutableState $showSuccessBar$delegate, SnackbarHostState $snackbarHostState, boolean isGranted) {
        if (!isGranted) {
            BuildersKt__Builders_commonKt.launch$default($scope, null, null, new SmsSendScreenKt$SmsSendScreen$permissionLauncher$1$1$2($snackbarHostState, null), 3, null);
        } else {
            $viewModel.sendSmsToTeachersOneByOne($context, new Function0() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$$ExternalSyntheticLambda6
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    Unit SmsSendScreen$lambda$27$lambda$26$lambda$25;
                    SmsSendScreen$lambda$27$lambda$26$lambda$25 = SmsSendScreenKt.SmsSendScreen$lambda$27$lambda$26$lambda$25(CoroutineScope.this, $successMessage$delegate, $showSuccessBar$delegate);
                    return SmsSendScreen$lambda$27$lambda$26$lambda$25;
                }
            });
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit SmsSendScreen$lambda$27$lambda$26$lambda$25(CoroutineScope $scope, MutableState $successMessage$delegate, MutableState $showSuccessBar$delegate) {
        SmsSendScreen$showSuccess($scope, $successMessage$delegate, $showSuccessBar$delegate, "SMS sent successfully");
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void SmsHistoryItem(final SmsViewModel.SmsMessage sms, Composer $composer, final int $changed) {
        Composer $composer2;
        Composer $composer3 = $composer.startRestartGroup(473537931);
        ComposerKt.sourceInformation($composer3, "C(SmsHistoryItem)667@34199L38,669@34287L4126,664@34103L4310:SmsSendScreen.kt#ersv7o");
        int $dirty = $changed;
        if (($changed & 6) == 0) {
            $dirty |= $composer3.changed(sms) ? 4 : 2;
        }
        if (($dirty & 3) != 2 || !$composer3.getSkipping()) {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(473537931, $dirty, -1, "com.substituemanagment.managment.ui.screens.SmsHistoryItem (SmsSendScreen.kt:663)");
            }
            CardKt.Card(SizeKt.fillMaxWidth$default(Modifier.Companion, 0.0f, 1, null), RoundedCornerShapeKt.m961RoundedCornerShape0680j_4(Dp.m6513constructorimpl(12)), null, CardDefaults.INSTANCE.m1833cardElevationaqJV_2Y(Dp.m6513constructorimpl(2), 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, $composer3, (CardDefaults.$stable << 18) | 6, 62), null, ComposableLambdaKt.rememberComposableLambda(1722398681, true, new Function3<ColumnScope, Composer, Integer, Unit>() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$SmsHistoryItem$1
                @Override // kotlin.jvm.functions.Function3
                public /* bridge */ /* synthetic */ Unit invoke(ColumnScope columnScope, Composer composer, Integer num) {
                    invoke(columnScope, composer, num.intValue());
                    return Unit.INSTANCE;
                }

                /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
                /* JADX WARN: Removed duplicated region for block: B:28:0x01d1  */
                /* JADX WARN: Removed duplicated region for block: B:31:0x01dd  */
                /* JADX WARN: Removed duplicated region for block: B:32:0x01e3  */
                /* JADX WARN: Removed duplicated region for block: B:43:0x02f8  */
                /* JADX WARN: Removed duplicated region for block: B:44:0x02fc  */
                /* JADX WARN: Removed duplicated region for block: B:48:0x0324  */
                /* JADX WARN: Removed duplicated region for block: B:52:0x034a  */
                /* JADX WARN: Removed duplicated region for block: B:59:0x03b1  */
                /* JADX WARN: Removed duplicated region for block: B:63:0x03e4  */
                /* JADX WARN: Removed duplicated region for block: B:67:0x0416  */
                /* JADX WARN: Removed duplicated region for block: B:70:0x0447  */
                /* JADX WARN: Removed duplicated region for block: B:73:0x0569  */
                /* JADX WARN: Removed duplicated region for block: B:76:? A[RETURN, SYNTHETIC] */
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                    To view partially-correct add '--show-bad-code' argument
                */
                public final void invoke(androidx.compose.foundation.layout.ColumnScope r87, androidx.compose.runtime.Composer r88, int r89) {
                    /*
                        Method dump skipped, instructions count: 1418
                        To view this dump add '--comments-level debug' option
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$SmsHistoryItem$1.invoke(androidx.compose.foundation.layout.ColumnScope, androidx.compose.runtime.Composer, int):void");
                }
            }, $composer3, 54), $composer3, 196614, 20);
            $composer2 = $composer3;
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        } else {
            $composer3.skipToGroupEnd();
            $composer2 = $composer3;
        }
        ScopeUpdateScope endRestartGroup = $composer2.endRestartGroup();
        if (endRestartGroup != null) {
            endRestartGroup.updateScope(new Function2() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$$ExternalSyntheticLambda4
                @Override // kotlin.jvm.functions.Function2
                public final Object invoke(Object obj, Object obj2) {
                    Unit SmsHistoryItem$lambda$31;
                    SmsHistoryItem$lambda$31 = SmsSendScreenKt.SmsHistoryItem$lambda$31(SmsViewModel.SmsMessage.this, $changed, (Composer) obj, ((Integer) obj2).intValue());
                    return SmsHistoryItem$lambda$31;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void ExpandableTeacherItem(final String teacherName, final boolean isSelected, final Function0<Unit> function0, final String assignmentsSummary, final boolean isExpanded, final Function0<Unit> function02, final String messageTemplate, final List<SmsViewModel.AssignmentData> list, final Function0<Unit> function03, Composer $composer, final int $changed) {
        String str;
        Object obj;
        Object obj2;
        Object obj3;
        Object obj4;
        Object obj5;
        Function0 function04;
        Composer $composer2;
        Composer $composer3 = $composer.startRestartGroup(-1732532107);
        ComposerKt.sourceInformation($composer3, "C(ExpandableTeacherItem)P(8,3,6,1,2,5,4)776@38886L20,779@39012L6555,772@38760L6807:SmsSendScreen.kt#ersv7o");
        int $dirty = $changed;
        if (($changed & 6) == 0) {
            str = teacherName;
            $dirty |= $composer3.changed(str) ? 4 : 2;
        } else {
            str = teacherName;
        }
        if (($changed & 48) == 0) {
            $dirty |= $composer3.changed(isSelected) ? 32 : 16;
        }
        if (($changed & 384) == 0) {
            obj = function0;
            $dirty |= $composer3.changedInstance(obj) ? 256 : 128;
        } else {
            obj = function0;
        }
        if (($changed & 3072) == 0) {
            obj2 = assignmentsSummary;
            $dirty |= $composer3.changed(obj2) ? 2048 : 1024;
        } else {
            obj2 = assignmentsSummary;
        }
        if (($changed & 24576) == 0) {
            $dirty |= $composer3.changed(isExpanded) ? 16384 : 8192;
        }
        if ((196608 & $changed) == 0) {
            $dirty |= $composer3.changedInstance(function02) ? 131072 : 65536;
        }
        if ((1572864 & $changed) == 0) {
            obj3 = messageTemplate;
            $dirty |= $composer3.changed(obj3) ? 1048576 : 524288;
        } else {
            obj3 = messageTemplate;
        }
        if ((12582912 & $changed) == 0) {
            obj4 = list;
            $dirty |= $composer3.changedInstance(obj4) ? 8388608 : 4194304;
        } else {
            obj4 = list;
        }
        if ((100663296 & $changed) == 0) {
            obj5 = function03;
            $dirty |= $composer3.changedInstance(obj5) ? AccessibilityEventCompat.TYPE_VIEW_TARGETED_BY_SCROLL : 33554432;
        } else {
            obj5 = function03;
        }
        int $dirty2 = $dirty;
        if ((38347923 & $dirty2) != 38347922 || !$composer3.getSkipping()) {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(-1732532107, $dirty2, -1, "com.substituemanagment.managment.ui.screens.ExpandableTeacherItem (SmsSendScreen.kt:771)");
            }
            Modifier m680paddingVpY3zN4$default = PaddingKt.m680paddingVpY3zN4$default(SizeKt.fillMaxWidth$default(Modifier.Companion, 0.0f, 1, null), 0.0f, Dp.m6513constructorimpl(4), 1, null);
            $composer3.startReplaceGroup(1745885955);
            ComposerKt.sourceInformation($composer3, "CC(remember):SmsSendScreen.kt#9igjgp");
            boolean z = (458752 & $dirty2) == 131072;
            Object rememberedValue = $composer3.rememberedValue();
            if (z || rememberedValue == Composer.Companion.getEmpty()) {
                function04 = new Function0() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$$ExternalSyntheticLambda2
                    @Override // kotlin.jvm.functions.Function0
                    public final Object invoke() {
                        Unit ExpandableTeacherItem$lambda$33$lambda$32;
                        ExpandableTeacherItem$lambda$33$lambda$32 = SmsSendScreenKt.ExpandableTeacherItem$lambda$33$lambda$32(Function0.this);
                        return ExpandableTeacherItem$lambda$33$lambda$32;
                    }
                };
                $composer3.updateRememberedValue(function04);
            } else {
                function04 = rememberedValue;
            }
            $composer3.endReplaceGroup();
            $composer2 = $composer3;
            SurfaceKt.m2520SurfaceT9BRK9s(ClickableKt.m266clickableXHw0xAI$default(m680paddingVpY3zN4$default, false, null, null, function04, 7, null), RoundedCornerShapeKt.m961RoundedCornerShape0680j_4(Dp.m6513constructorimpl(8)), 0L, 0L, isExpanded ? Dp.m6513constructorimpl(4) : Dp.m6513constructorimpl(0), 0.0f, null, ComposableLambdaKt.rememberComposableLambda(-1857277062, true, new SmsSendScreenKt$ExpandableTeacherItem$2(isExpanded, obj, isSelected, str, obj2, obj4, obj5, obj3), $composer3, 54), $composer2, 12582912, 108);
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        } else {
            $composer3.skipToGroupEnd();
            $composer2 = $composer3;
        }
        ScopeUpdateScope endRestartGroup = $composer2.endRestartGroup();
        if (endRestartGroup != null) {
            endRestartGroup.updateScope(new Function2() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$$ExternalSyntheticLambda3
                @Override // kotlin.jvm.functions.Function2
                public final Object invoke(Object obj6, Object obj7) {
                    Unit ExpandableTeacherItem$lambda$34;
                    ExpandableTeacherItem$lambda$34 = SmsSendScreenKt.ExpandableTeacherItem$lambda$34(teacherName, isSelected, function0, assignmentsSummary, isExpanded, function02, messageTemplate, list, function03, $changed, (Composer) obj6, ((Integer) obj7).intValue());
                    return ExpandableTeacherItem$lambda$34;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit ExpandableTeacherItem$lambda$33$lambda$32(Function0 $onExpandToggle) {
        $onExpandToggle.invoke();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final String createPreviewMessage(String template, String teacherName, SmsViewModel.AssignmentData assignment) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        Date today = new Date();
        String replace$default = StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(template, "{substitute}", teacherName, false, 4, (Object) null), "{class}", assignment.getClassName(), false, 4, (Object) null), "{period}", String.valueOf(assignment.getPeriod()), false, 4, (Object) null);
        String format = dateFormat.format(today);
        Intrinsics.checkNotNullExpressionValue(format, "format(...)");
        String replace$default2 = StringsKt.replace$default(replace$default, "{date}", format, false, 4, (Object) null);
        String format2 = dayFormat.format(today);
        Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
        return StringsKt.replace$default(StringsKt.replace$default(replace$default2, "{day}", format2, false, 4, (Object) null), "{original_teacher}", assignment.getOriginalTeacher(), false, 4, (Object) null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final String getAssignmentsSummary(List<SmsViewModel.AssignmentData> list) {
        if (list.isEmpty()) {
            return "No assignments";
        }
        List sortedAssignments = CollectionsKt.sortedWith(list, new Comparator() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$getAssignmentsSummary$$inlined$sortedBy$1
            @Override // java.util.Comparator
            public final int compare(T t, T t2) {
                return ComparisonsKt.compareValues(Integer.valueOf(((SmsViewModel.AssignmentData) t).getPeriod()), Integer.valueOf(((SmsViewModel.AssignmentData) t2).getPeriod()));
            }
        });
        if (sortedAssignments.size() == 1) {
            SmsViewModel.AssignmentData assignment = (SmsViewModel.AssignmentData) CollectionsKt.first((List<? extends Object>) sortedAssignments);
            int period = assignment.getPeriod();
            String className = assignment.getClassName();
            return "Period " + period + ": " + className + " (for " + assignment.getOriginalTeacher() + ")";
        }
        int size = sortedAssignments.size();
        return "Assigned to " + size + " classes: " + CollectionsKt.joinToString$default(sortedAssignments, ", ", null, null, 0, null, new Function1() { // from class: com.substituemanagment.managment.ui.screens.SmsSendScreenKt$$ExternalSyntheticLambda5
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                CharSequence assignmentsSummary$lambda$36;
                assignmentsSummary$lambda$36 = SmsSendScreenKt.getAssignmentsSummary$lambda$36((SmsViewModel.AssignmentData) obj);
                return assignmentsSummary$lambda$36;
            }
        }, 30, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence getAssignmentsSummary$lambda$36(SmsViewModel.AssignmentData it) {
        Intrinsics.checkNotNullParameter(it, "it");
        return "P" + it.getPeriod();
    }
}

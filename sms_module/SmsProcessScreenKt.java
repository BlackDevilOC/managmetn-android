package com.substituemanagment.managment.ui.screens;

import android.content.Context;
import androidx.autofill.HintConstants;
import androidx.compose.foundation.layout.PaddingKt;
import androidx.compose.foundation.layout.RowScope;
import androidx.compose.foundation.layout.SizeKt;
import androidx.compose.material3.AndroidAlertDialog_androidKt;
import androidx.compose.material3.AppBarKt;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.CardElevation;
import androidx.compose.material3.CardKt;
import androidx.compose.material3.ScaffoldKt;
import androidx.compose.material3.SnackbarHostKt;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.runtime.Composer;
import androidx.compose.runtime.ComposerKt;
import androidx.compose.runtime.CompositionScopedCoroutineScopeCanceller;
import androidx.compose.runtime.EffectsKt;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.RecomposeScopeImplKt;
import androidx.compose.runtime.ScopeUpdateScope;
import androidx.compose.runtime.SnapshotStateKt;
import androidx.compose.runtime.SnapshotStateKt__SnapshotStateKt;
import androidx.compose.runtime.State;
import androidx.compose.runtime.internal.ComposableLambdaKt;
import androidx.compose.runtime.snapshots.SnapshotStateList;
import androidx.compose.runtime.snapshots.SnapshotStateMap;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.platform.AndroidCompositionLocals_androidKt;
import androidx.compose.ui.unit.Dp;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner;
import androidx.lifecycle.viewmodel.compose.ViewModelKt;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.profileinstaller.ProfileVerifier;
import com.substituemanagment.managment.navigation.Screen;
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel;
import com.substituemanagment.managment.ui.viewmodels.TeacherDetailsViewModel;
import java.util.Comparator;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import kotlin.text.Regex;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt__Builders_commonKt;
import kotlinx.coroutines.CoroutineScope;
/* compiled from: SmsProcessScreen.kt */
@Metadata(d1 = {"\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\b\n\u0002\b\u0003\u001a\u0015\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0007¢\u0006\u0002\u0010\u0004\u001a\u0016\u0010\u0005\u001a\u00020\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bH\u0002\u001a\u0081\u0001\u0010\n\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00102\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0013\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00122\u0006\u0010\u0014\u001a\u00020\u000e2\b\b\u0002\u0010\u0015\u001a\u00020\u00062\b\b\u0002\u0010\u0016\u001a\u00020\u000e2\b\b\u0002\u0010\u0017\u001a\u00020\u000eH\u0007¢\u0006\u0002\u0010\u0018\u001a\u000e\u0010\u0019\u001a\u00020\u000e2\u0006\u0010\u001a\u001a\u00020\u0006¨\u0006\u001b²\u0006\n\u0010\u001c\u001a\u00020\u000eX\u008a\u0084\u0002²\u0006\f\u0010\u001d\u001a\u0004\u0018\u00010\u0006X\u008a\u0084\u0002²\u0006\n\u0010\u001e\u001a\u00020\u000eX\u008a\u008e\u0002²\u0006\n\u0010\u001f\u001a\u00020\u0006X\u008a\u008e\u0002²\u0006\n\u0010 \u001a\u00020\u000eX\u008a\u008e\u0002²\u0006\n\u0010!\u001a\u00020\"X\u008a\u008e\u0002²\u0006\n\u0010#\u001a\u00020\u000eX\u008a\u008e\u0002²\u0006\f\u0010$\u001a\u0004\u0018\u00010\u0006X\u008a\u008e\u0002²\u0006\n\u0010%\u001a\u00020\u0006X\u008a\u008e\u0002"}, d2 = {"SmsProcessScreen", "", "navController", "Landroidx/navigation/NavController;", "(Landroidx/navigation/NavController;Landroidx/compose/runtime/Composer;I)V", "getSummaryForAssignments", "", "assignments", "", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$AssignmentData;", "TeacherPhoneVerificationItem", "teacherName", HintConstants.AUTOFILL_HINT_PHONE_NUMBER, "isEditing", "", "onEditClick", "Lkotlin/Function0;", "onPhoneChange", "Lkotlin/Function1;", "onDone", "isValid", "assignmentsSummary", "isAutoLoaded", "hadValidPhoneInitially", "(Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;ZLjava/lang/String;ZZLandroidx/compose/runtime/Composer;II)V", "isValidPhoneNumber", HintConstants.AUTOFILL_HINT_PHONE, "app_debug", "isLoading", "errorMessage", "showSuccessBar", "successMessage", "showAutoLoadedInfo", "autoLoadedCount", "", "showSaveNumberDialog", "currentTeacherForSaving", "currentNumberForSaving"}, k = 2, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes9.dex */
public final class SmsProcessScreenKt {
    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit SmsProcessScreen$lambda$35(NavController navController, int i, Composer composer, int i2) {
        SmsProcessScreen(navController, composer, RecomposeScopeImplKt.updateChangedFlags(i | 1));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit TeacherPhoneVerificationItem$lambda$38(String str, String str2, boolean z, Function0 function0, Function1 function1, Function1 function12, boolean z2, String str3, boolean z3, boolean z4, int i, int i2, Composer composer, int i3) {
        TeacherPhoneVerificationItem(str, str2, z, function0, function1, function12, z2, str3, z3, z4, composer, RecomposeScopeImplKt.updateChangedFlags(i | 1), i2);
        return Unit.INSTANCE;
    }

    public static final void SmsProcessScreen(final NavController navController, Composer $composer, final int $changed) {
        CreationExtras creationExtras;
        CreationExtras creationExtras2;
        int $dirty;
        Object obj;
        Object obj2;
        Object obj3;
        Object obj4;
        Object obj5;
        State<Boolean> state;
        State<String> state2;
        Object obj6;
        SnapshotStateList selectedTeachers;
        SnackbarHostState snackbarHostState;
        Object obj7;
        Object obj8;
        Object obj9;
        Object obj10;
        Object obj11;
        MutableState showSaveNumberDialog$delegate;
        Object obj12;
        Object obj13;
        Object obj14;
        int i;
        State errorMessage$delegate;
        SnapshotStateList selectedTeachers2;
        TeacherDetailsViewModel teacherDetailsViewModel;
        SnapshotStateMap phoneNumbers;
        Unit unit;
        SmsViewModel viewModel;
        Context context;
        SnackbarHostState snackbarHostState2;
        SnapshotStateMap editingPhoneNumber;
        SnapshotStateMap originalPhoneStatus;
        MutableState autoLoadedCount$delegate;
        MutableState showAutoLoadedInfo$delegate;
        Object obj15;
        SnapshotStateMap editingPhoneNumber2;
        State errorMessage$delegate2;
        SmsViewModel viewModel2;
        SmsProcessScreenKt$SmsProcessScreen$3$1 smsProcessScreenKt$SmsProcessScreen$3$1;
        final SnackbarHostState snackbarHostState3;
        Composer $composer2;
        MutableState showSaveNumberDialog$delegate2;
        final MutableState currentTeacherForSaving$delegate;
        CoroutineScope scope;
        final MutableState currentNumberForSaving$delegate;
        Context context2;
        Composer $composer3;
        final MutableState showSaveNumberDialog$delegate3;
        Object obj16;
        Intrinsics.checkNotNullParameter(navController, "navController");
        Composer $composer4 = $composer.startRestartGroup(-814050879);
        ComposerKt.sourceInformation($composer4, "C(SmsProcessScreen)47@2146L11,48@2217L11,49@2260L7,50@2284L24,53@2379L70,54@2479L49,55@2552L48,58@2705L49,60@2781L32,61@2838L35,62@2902L32,63@2961L34,64@3022L31,67@3161L34,68@3223L30,71@3353L34,72@3423L42,73@3500L31,76@3604L59,76@3583L80,81@3759L2989,81@3738L3010,156@7121L102,156@7092L131,228@9874L1010,301@13290L35,253@10917L2348,302@13332L9545,227@9847L13030:SmsProcessScreen.kt#ersv7o");
        int $dirty2 = $changed;
        if (($changed & 6) == 0) {
            $dirty2 |= $composer4.changedInstance(navController) ? 4 : 2;
        }
        if (($dirty2 & 3) != 2 || !$composer4.getSkipping()) {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(-814050879, $dirty2, -1, "com.substituemanagment.managment.ui.screens.SmsProcessScreen (SmsProcessScreen.kt:46)");
            }
            $composer4.startReplaceableGroup(1729797275);
            ComposerKt.sourceInformation($composer4, "CC(viewModel)P(3,2,1)*54@2502L7,64@2877L63:ViewModel.kt#3tja67");
            ViewModelStoreOwner current = LocalViewModelStoreOwner.INSTANCE.getCurrent($composer4, 6);
            if (current == null) {
                throw new IllegalStateException("No ViewModelStoreOwner was provided via LocalViewModelStoreOwner".toString());
            }
            if (current instanceof HasDefaultViewModelProviderFactory) {
                creationExtras = ((HasDefaultViewModelProviderFactory) current).getDefaultViewModelCreationExtras();
            } else {
                creationExtras = CreationExtras.Empty.INSTANCE;
            }
            ViewModel viewModel3 = ViewModelKt.viewModel(Reflection.getOrCreateKotlinClass(SmsViewModel.class), current, (String) null, (ViewModelProvider.Factory) null, creationExtras, $composer4, ((0 << 3) & 112) | ((0 << 3) & 896) | ((0 << 3) & 7168) | ((0 << 3) & 57344), 0);
            $composer4.endReplaceableGroup();
            SmsViewModel viewModel4 = (SmsViewModel) viewModel3;
            $composer4.startReplaceableGroup(1729797275);
            ComposerKt.sourceInformation($composer4, "CC(viewModel)P(3,2,1)*54@2502L7,64@2877L63:ViewModel.kt#3tja67");
            ViewModelStoreOwner current2 = LocalViewModelStoreOwner.INSTANCE.getCurrent($composer4, 6);
            if (current2 == null) {
                throw new IllegalStateException("No ViewModelStoreOwner was provided via LocalViewModelStoreOwner".toString());
            }
            if (current2 instanceof HasDefaultViewModelProviderFactory) {
                creationExtras2 = ((HasDefaultViewModelProviderFactory) current2).getDefaultViewModelCreationExtras();
            } else {
                creationExtras2 = CreationExtras.Empty.INSTANCE;
            }
            ViewModel viewModel5 = ViewModelKt.viewModel(Reflection.getOrCreateKotlinClass(TeacherDetailsViewModel.class), current2, (String) null, (ViewModelProvider.Factory) null, creationExtras2, $composer4, ((0 << 3) & 112) | ((0 << 3) & 896) | ((0 << 3) & 7168) | ((0 << 3) & 57344), 0);
            $composer4.endReplaceableGroup();
            TeacherDetailsViewModel teacherDetailsViewModel2 = (TeacherDetailsViewModel) viewModel5;
            ComposerKt.sourceInformationMarkerStart($composer4, 2023513938, "CC:CompositionLocal.kt#9igjgp");
            Object consume = $composer4.consume(AndroidCompositionLocals_androidKt.getLocalContext());
            ComposerKt.sourceInformationMarkerEnd($composer4);
            Context context3 = (Context) consume;
            ComposerKt.sourceInformationMarkerStart($composer4, 773894976, "CC(rememberCoroutineScope)482@20332L144:Effects.kt#9igjgp");
            ComposerKt.sourceInformationMarkerStart($composer4, -954367824, "CC(remember):Effects.kt#9igjgp");
            Object rememberedValue = $composer4.rememberedValue();
            if (rememberedValue == Composer.Companion.getEmpty()) {
                $dirty = $dirty2;
                obj = new CompositionScopedCoroutineScopeCanceller(EffectsKt.createCompositionCoroutineScope(EmptyCoroutineContext.INSTANCE, $composer4));
                $composer4.updateRememberedValue(obj);
            } else {
                $dirty = $dirty2;
                obj = rememberedValue;
            }
            ComposerKt.sourceInformationMarkerEnd($composer4);
            CoroutineScope scope2 = ((CompositionScopedCoroutineScopeCanceller) obj).getCoroutineScope();
            ComposerKt.sourceInformationMarkerEnd($composer4);
            $composer4.startReplaceGroup(579539802);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue2 = $composer4.rememberedValue();
            if (rememberedValue2 == Composer.Companion.getEmpty()) {
                obj2 = SnapshotStateKt.mutableStateListOf();
                $composer4.updateRememberedValue(obj2);
            } else {
                obj2 = rememberedValue2;
            }
            SnapshotStateList selectedTeachers3 = (SnapshotStateList) obj2;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579542981);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue3 = $composer4.rememberedValue();
            if (rememberedValue3 == Composer.Companion.getEmpty()) {
                obj3 = SnapshotStateKt.mutableStateMapOf();
                $composer4.updateRememberedValue(obj3);
            } else {
                obj3 = rememberedValue3;
            }
            SnapshotStateMap editingPhoneNumber3 = (SnapshotStateMap) obj3;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579545316);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue4 = $composer4.rememberedValue();
            if (rememberedValue4 == Composer.Companion.getEmpty()) {
                obj4 = SnapshotStateKt.mutableStateMapOf();
                $composer4.updateRememberedValue(obj4);
            } else {
                obj4 = rememberedValue4;
            }
            SnapshotStateMap phoneNumbers2 = (SnapshotStateMap) obj4;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579550213);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue5 = $composer4.rememberedValue();
            if (rememberedValue5 == Composer.Companion.getEmpty()) {
                obj5 = SnapshotStateKt.mutableStateMapOf();
                $composer4.updateRememberedValue(obj5);
            } else {
                obj5 = rememberedValue5;
            }
            SnapshotStateMap originalPhoneStatus2 = (SnapshotStateMap) obj5;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579552628);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue6 = $composer4.rememberedValue();
            if (rememberedValue6 == Composer.Companion.getEmpty()) {
                state = viewModel4.isLoading();
                $composer4.updateRememberedValue(state);
            } else {
                state = rememberedValue6;
            }
            State isLoading$delegate = state;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579554455);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue7 = $composer4.rememberedValue();
            if (rememberedValue7 == Composer.Companion.getEmpty()) {
                state2 = viewModel4.getErrorMessage();
                $composer4.updateRememberedValue(state2);
            } else {
                state2 = rememberedValue7;
            }
            State errorMessage$delegate3 = state2;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579556500);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue8 = $composer4.rememberedValue();
            if (rememberedValue8 == Composer.Companion.getEmpty()) {
                obj6 = new SnackbarHostState();
                $composer4.updateRememberedValue(obj6);
            } else {
                obj6 = rememberedValue8;
            }
            SnackbarHostState snackbarHostState4 = (SnackbarHostState) obj6;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579558390);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue9 = $composer4.rememberedValue();
            if (rememberedValue9 == Composer.Companion.getEmpty()) {
                selectedTeachers = selectedTeachers3;
                snackbarHostState = snackbarHostState4;
                obj7 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(false, null, 2, null);
                $composer4.updateRememberedValue(obj7);
            } else {
                selectedTeachers = selectedTeachers3;
                snackbarHostState = snackbarHostState4;
                obj7 = rememberedValue9;
            }
            MutableState showSuccessBar$delegate = (MutableState) obj7;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579560339);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue10 = $composer4.rememberedValue();
            if (rememberedValue10 == Composer.Companion.getEmpty()) {
                obj8 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default("", null, 2, null);
                $composer4.updateRememberedValue(obj8);
            } else {
                obj8 = rememberedValue10;
            }
            MutableState successMessage$delegate = (MutableState) obj8;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579564790);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue11 = $composer4.rememberedValue();
            if (rememberedValue11 == Composer.Companion.getEmpty()) {
                obj9 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(false, null, 2, null);
                $composer4.updateRememberedValue(obj9);
            } else {
                obj9 = rememberedValue11;
            }
            MutableState showAutoLoadedInfo$delegate2 = (MutableState) obj9;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579566770);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue12 = $composer4.rememberedValue();
            if (rememberedValue12 == Composer.Companion.getEmpty()) {
                obj10 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(0, null, 2, null);
                $composer4.updateRememberedValue(obj10);
            } else {
                obj10 = rememberedValue12;
            }
            MutableState autoLoadedCount$delegate2 = (MutableState) obj10;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579570934);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue13 = $composer4.rememberedValue();
            if (rememberedValue13 == Composer.Companion.getEmpty()) {
                obj11 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(false, null, 2, null);
                $composer4.updateRememberedValue(obj11);
            } else {
                obj11 = rememberedValue13;
            }
            MutableState showSaveNumberDialog$delegate4 = (MutableState) obj11;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579573182);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue14 = $composer4.rememberedValue();
            if (rememberedValue14 == Composer.Companion.getEmpty()) {
                showSaveNumberDialog$delegate = showSaveNumberDialog$delegate4;
                obj12 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(null, null, 2, null);
                $composer4.updateRememberedValue(obj12);
            } else {
                showSaveNumberDialog$delegate = showSaveNumberDialog$delegate4;
                obj12 = rememberedValue14;
            }
            MutableState currentTeacherForSaving$delegate2 = (MutableState) obj12;
            $composer4.endReplaceGroup();
            $composer4.startReplaceGroup(579575635);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue15 = $composer4.rememberedValue();
            if (rememberedValue15 == Composer.Companion.getEmpty()) {
                obj13 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default("", null, 2, null);
                $composer4.updateRememberedValue(obj13);
            } else {
                obj13 = rememberedValue15;
            }
            MutableState currentNumberForSaving$delegate2 = (MutableState) obj13;
            $composer4.endReplaceGroup();
            Unit unit2 = Unit.INSTANCE;
            $composer4.startReplaceGroup(579578991);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            boolean changedInstance = $composer4.changedInstance(teacherDetailsViewModel2) | $composer4.changedInstance(context3);
            Object rememberedValue16 = $composer4.rememberedValue();
            if (changedInstance || rememberedValue16 == Composer.Companion.getEmpty()) {
                obj14 = (Function2) new SmsProcessScreenKt$SmsProcessScreen$1$1(teacherDetailsViewModel2, context3, null);
                $composer4.updateRememberedValue(obj14);
            } else {
                obj14 = rememberedValue16;
            }
            $composer4.endReplaceGroup();
            EffectsKt.LaunchedEffect(unit2, (Function2) obj14, $composer4, 6);
            Unit unit3 = Unit.INSTANCE;
            $composer4.startReplaceGroup(579586881);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            boolean changed = $composer4.changed(viewModel4) | $composer4.changedInstance(navController);
            Object rememberedValue17 = $composer4.rememberedValue();
            if (changed || rememberedValue17 == Composer.Companion.getEmpty()) {
                i = 6;
                errorMessage$delegate = errorMessage$delegate3;
                selectedTeachers2 = selectedTeachers;
                teacherDetailsViewModel = teacherDetailsViewModel2;
                phoneNumbers = phoneNumbers2;
                unit = unit3;
                viewModel = viewModel4;
                context = context3;
                snackbarHostState2 = snackbarHostState;
                editingPhoneNumber = editingPhoneNumber3;
                originalPhoneStatus = originalPhoneStatus2;
                autoLoadedCount$delegate = autoLoadedCount$delegate2;
                showAutoLoadedInfo$delegate = showAutoLoadedInfo$delegate2;
                obj15 = (Function2) new SmsProcessScreenKt$SmsProcessScreen$2$1(viewModel, selectedTeachers2, snackbarHostState2, navController, phoneNumbers, editingPhoneNumber3, originalPhoneStatus2, autoLoadedCount$delegate2, showAutoLoadedInfo$delegate2, null);
                $composer4.updateRememberedValue(obj15);
            } else {
                i = 6;
                context = context3;
                errorMessage$delegate = errorMessage$delegate3;
                selectedTeachers2 = selectedTeachers;
                showAutoLoadedInfo$delegate = showAutoLoadedInfo$delegate2;
                autoLoadedCount$delegate = autoLoadedCount$delegate2;
                editingPhoneNumber = editingPhoneNumber3;
                originalPhoneStatus = originalPhoneStatus2;
                teacherDetailsViewModel = teacherDetailsViewModel2;
                snackbarHostState2 = snackbarHostState;
                unit = unit3;
                phoneNumbers = phoneNumbers2;
                viewModel = viewModel4;
                obj15 = rememberedValue17;
            }
            $composer4.endReplaceGroup();
            EffectsKt.LaunchedEffect(unit, (Function2) obj15, $composer4, i);
            String SmsProcessScreen$lambda$7 = SmsProcessScreen$lambda$7(errorMessage$delegate);
            $composer4.startReplaceGroup(579691578);
            ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
            Object rememberedValue18 = $composer4.rememberedValue();
            if (rememberedValue18 == Composer.Companion.getEmpty()) {
                editingPhoneNumber2 = editingPhoneNumber;
                errorMessage$delegate2 = errorMessage$delegate;
                viewModel2 = viewModel;
                smsProcessScreenKt$SmsProcessScreen$3$1 = new SmsProcessScreenKt$SmsProcessScreen$3$1(errorMessage$delegate2, snackbarHostState2, null);
                $composer4.updateRememberedValue(smsProcessScreenKt$SmsProcessScreen$3$1);
            } else {
                editingPhoneNumber2 = editingPhoneNumber;
                errorMessage$delegate2 = errorMessage$delegate;
                viewModel2 = viewModel;
                smsProcessScreenKt$SmsProcessScreen$3$1 = rememberedValue18;
            }
            $composer4.endReplaceGroup();
            EffectsKt.LaunchedEffect(SmsProcessScreen$lambda$7, (Function2) smsProcessScreenKt$SmsProcessScreen$3$1, $composer4, 0);
            $composer4.startReplaceGroup(579699648);
            ComposerKt.sourceInformation($composer4, "165@7412L62,187@8385L950,208@9365L232,171@7576L779,164@7368L2463");
            if (!SmsProcessScreen$lambda$22(showSaveNumberDialog$delegate) || SmsProcessScreen$lambda$25(currentTeacherForSaving$delegate2) == null) {
                snackbarHostState3 = snackbarHostState2;
                $composer2 = $composer4;
                showSaveNumberDialog$delegate2 = showSaveNumberDialog$delegate;
                currentTeacherForSaving$delegate = currentTeacherForSaving$delegate2;
                scope = scope2;
                currentNumberForSaving$delegate = currentNumberForSaving$delegate2;
                context2 = context;
            } else {
                $composer4.startReplaceGroup(579700850);
                ComposerKt.sourceInformation($composer4, "CC(remember):SmsProcessScreen.kt#9igjgp");
                Object rememberedValue19 = $composer4.rememberedValue();
                if (rememberedValue19 == Composer.Companion.getEmpty()) {
                    showSaveNumberDialog$delegate3 = showSaveNumberDialog$delegate;
                    obj16 = new Function0() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$$ExternalSyntheticLambda0
                        @Override // kotlin.jvm.functions.Function0
                        public final Object invoke() {
                            Unit SmsProcessScreen$lambda$34$lambda$33;
                            SmsProcessScreen$lambda$34$lambda$33 = SmsProcessScreenKt.SmsProcessScreen$lambda$34$lambda$33(MutableState.this);
                            return SmsProcessScreen$lambda$34$lambda$33;
                        }
                    };
                    $composer4.updateRememberedValue(obj16);
                } else {
                    showSaveNumberDialog$delegate3 = showSaveNumberDialog$delegate;
                    obj16 = rememberedValue19;
                }
                $composer4.endReplaceGroup();
                SnackbarHostState snackbarHostState5 = snackbarHostState2;
                MutableState showSaveNumberDialog$delegate5 = showSaveNumberDialog$delegate3;
                scope = scope2;
                currentTeacherForSaving$delegate = currentTeacherForSaving$delegate2;
                currentNumberForSaving$delegate = currentNumberForSaving$delegate2;
                snackbarHostState3 = snackbarHostState5;
                showSaveNumberDialog$delegate2 = showSaveNumberDialog$delegate5;
                context2 = context;
                AndroidAlertDialog_androidKt.m1769AlertDialogOix01E0((Function0) obj16, ComposableLambdaKt.rememberComposableLambda(1926325966, true, new SmsProcessScreenKt$SmsProcessScreen$5(teacherDetailsViewModel, scope2, currentTeacherForSaving$delegate2, currentNumberForSaving$delegate2, snackbarHostState5, showSaveNumberDialog$delegate5), $composer4, 54), null, ComposableLambdaKt.rememberComposableLambda(1770345424, true, new SmsProcessScreenKt$SmsProcessScreen$6(showSaveNumberDialog$delegate5), $composer4, 54), ComposableSingletons$SmsProcessScreenKt.INSTANCE.m6933getLambda3$app_debug(), ComposableSingletons$SmsProcessScreenKt.INSTANCE.m6934getLambda4$app_debug(), ComposableLambdaKt.rememberComposableLambda(1536374611, true, new Function2<Composer, Integer, Unit>() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$7
                    @Override // kotlin.jvm.functions.Function2
                    public /* bridge */ /* synthetic */ Unit invoke(Composer composer, Integer num) {
                        invoke(composer, num.intValue());
                        return Unit.INSTANCE;
                    }

                    /* JADX WARN: Removed duplicated region for block: B:28:0x021a  */
                    /* JADX WARN: Removed duplicated region for block: B:31:? A[RETURN, SYNTHETIC] */
                    /*
                        Code decompiled incorrectly, please refer to instructions dump.
                        To view partially-correct add '--show-bad-code' argument
                    */
                    public final void invoke(androidx.compose.runtime.Composer r53, int r54) {
                        /*
                            Method dump skipped, instructions count: 542
                            To view this dump add '--comments-level debug' option
                        */
                        throw new UnsupportedOperationException("Method not decompiled: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$7.invoke(androidx.compose.runtime.Composer, int):void");
                    }
                }, $composer4, 54), null, 0L, 0L, 0L, 0L, 0.0f, null, $composer4, 1797174, 0, 16260);
                $composer2 = $composer4;
            }
            $composer2.endReplaceGroup();
            SmsViewModel viewModel6 = viewModel2;
            SnapshotStateMap phoneNumbers3 = phoneNumbers;
            SnapshotStateList selectedTeachers4 = selectedTeachers2;
            State errorMessage$delegate4 = errorMessage$delegate2;
            Composer $composer5 = $composer2;
            ScaffoldKt.m2323ScaffoldTvnljyQ(null, ComposableLambdaKt.rememberComposableLambda(2005240957, true, new Function2<Composer, Integer, Unit>() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8
                @Override // kotlin.jvm.functions.Function2
                public /* bridge */ /* synthetic */ Unit invoke(Composer composer, Integer num) {
                    invoke(composer, num.intValue());
                    return Unit.INSTANCE;
                }

                public final void invoke(Composer $composer6, int $changed2) {
                    ComposerKt.sourceInformation($composer6, "C231@9994L217,236@10239L621,229@9888L986:SmsProcessScreen.kt#ersv7o");
                    if (($changed2 & 3) != 2 || !$composer6.getSkipping()) {
                        if (ComposerKt.isTraceInProgress()) {
                            ComposerKt.traceEventStart(2005240957, $changed2, -1, "com.substituemanagment.managment.ui.screens.SmsProcessScreen.<anonymous> (SmsProcessScreen.kt:229)");
                        }
                        AppBarKt.TopAppBar(ComposableSingletons$SmsProcessScreenKt.INSTANCE.m6935getLambda5$app_debug(), null, ComposableLambdaKt.rememberComposableLambda(-1247019837, true, new AnonymousClass1(NavController.this), $composer6, 54), ComposableLambdaKt.rememberComposableLambda(-833288916, true, new AnonymousClass2(NavController.this), $composer6, 54), null, null, null, $composer6, 3462, 114);
                        if (ComposerKt.isTraceInProgress()) {
                            ComposerKt.traceEventEnd();
                            return;
                        }
                        return;
                    }
                    $composer6.skipToGroupEnd();
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                /* compiled from: SmsProcessScreen.kt */
                @Metadata(k = 3, mv = {2, 0, 0}, xi = 48)
                /* renamed from: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$1  reason: invalid class name */
                /* loaded from: classes9.dex */
                public static final class AnonymousClass1 implements Function2<Composer, Integer, Unit> {
                    final /* synthetic */ NavController $navController;

                    AnonymousClass1(NavController navController) {
                        this.$navController = navController;
                    }

                    @Override // kotlin.jvm.functions.Function2
                    public /* bridge */ /* synthetic */ Unit invoke(Composer composer, Integer num) {
                        invoke(composer, num.intValue());
                        return Unit.INSTANCE;
                    }

                    /* JADX INFO: Access modifiers changed from: private */
                    public static final Unit invoke$lambda$1$lambda$0(NavController $navController) {
                        NavController.navigate$default($navController, Screen.SmsSend.INSTANCE.getRoute(), (NavOptions) null, (Navigator.Extras) null, 6, (Object) null);
                        return Unit.INSTANCE;
                    }

                    public final void invoke(Composer $composer, int $changed) {
                        Function0 function0;
                        ComposerKt.sourceInformation($composer, "C232@10037L48,232@10016L177:SmsProcessScreen.kt#ersv7o");
                        if (($changed & 3) == 2 && $composer.getSkipping()) {
                            $composer.skipToGroupEnd();
                            return;
                        }
                        if (ComposerKt.isTraceInProgress()) {
                            ComposerKt.traceEventStart(-1247019837, $changed, -1, "com.substituemanagment.managment.ui.screens.SmsProcessScreen.<anonymous>.<anonymous> (SmsProcessScreen.kt:232)");
                        }
                        $composer.startReplaceGroup(1326871042);
                        ComposerKt.sourceInformation($composer, "CC(remember):SmsProcessScreen.kt#9igjgp");
                        boolean changedInstance = $composer.changedInstance(this.$navController);
                        final NavController navController = this.$navController;
                        Object rememberedValue = $composer.rememberedValue();
                        if (changedInstance || rememberedValue == Composer.Companion.getEmpty()) {
                            function0 = 
                            /*  JADX ERROR: Method code generation error
                                jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x004e: CONSTRUCTOR  (r8v0 'function0' kotlin.jvm.functions.Function0) = (r1v1 'navController' androidx.navigation.NavController A[DONT_INLINE]) call: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$1$$ExternalSyntheticLambda0.<init>(androidx.navigation.NavController):void type: CONSTRUCTOR in method: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8.1.invoke(androidx.compose.runtime.Composer, int):void, file: classes9.dex
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:309)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:272)
                                	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:91)
                                	at jadx.core.dex.nodes.IBlock.generate(IBlock.java:15)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:80)
                                	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:123)
                                	at jadx.core.dex.regions.conditions.IfRegion.generate(IfRegion.java:90)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:296)
                                	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:275)
                                	at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:377)
                                	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:306)
                                	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:272)
                                	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                                	at java.base/java.util.ArrayList.forEach(ArrayList.java:1541)
                                	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                                	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                                Caused by: jadx.core.utils.exceptions.JadxRuntimeException: Expected class to be processed at this point, class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$1$$ExternalSyntheticLambda0, state: NOT_LOADED
                                	at jadx.core.dex.nodes.ClassNode.ensureProcessed(ClassNode.java:302)
                                	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:769)
                                	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:718)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:417)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:302)
                                	... 25 more
                                */
                            /*
                                this = this;
                                java.lang.String r0 = "C232@10037L48,232@10016L177:SmsProcessScreen.kt#ersv7o"
                                androidx.compose.runtime.ComposerKt.sourceInformation(r10, r0)
                                r0 = r11 & 3
                                r1 = 2
                                if (r0 != r1) goto L15
                                boolean r0 = r10.getSkipping()
                                if (r0 != 0) goto L11
                                goto L15
                            L11:
                                r10.skipToGroupEnd()
                                goto L79
                            L15:
                                boolean r0 = androidx.compose.runtime.ComposerKt.isTraceInProgress()
                                if (r0 == 0) goto L24
                                r0 = -1
                                java.lang.String r1 = "com.substituemanagment.managment.ui.screens.SmsProcessScreen.<anonymous>.<anonymous> (SmsProcessScreen.kt:232)"
                                r2 = -1247019837(0xffffffffb5abfcc3, float:-1.2814056E-6)
                                androidx.compose.runtime.ComposerKt.traceEventStart(r2, r11, r0, r1)
                            L24:
                                r0 = 1326871042(0x4f167202, float:2.52405402E9)
                                r10.startReplaceGroup(r0)
                                java.lang.String r0 = "CC(remember):SmsProcessScreen.kt#9igjgp"
                                androidx.compose.runtime.ComposerKt.sourceInformation(r10, r0)
                                androidx.navigation.NavController r0 = r9.$navController
                                boolean r0 = r10.changedInstance(r0)
                                androidx.navigation.NavController r1 = r9.$navController
                                r2 = r10
                                r3 = 0
                                java.lang.Object r4 = r2.rememberedValue()
                                r5 = 0
                                if (r0 != 0) goto L4b
                                androidx.compose.runtime.Composer$Companion r7 = androidx.compose.runtime.Composer.Companion
                                java.lang.Object r7 = r7.getEmpty()
                                if (r4 != r7) goto L49
                                goto L4b
                            L49:
                                r8 = r4
                                goto L56
                            L4b:
                                r7 = 0
                                com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$1$$ExternalSyntheticLambda0 r8 = new com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$1$$ExternalSyntheticLambda0
                                r8.<init>(r1)
                                r2.updateRememberedValue(r8)
                            L56:
                                r0 = r8
                                kotlin.jvm.functions.Function0 r0 = (kotlin.jvm.functions.Function0) r0
                                r10.endReplaceGroup()
                                com.substituemanagment.managment.ui.screens.ComposableSingletons$SmsProcessScreenKt r1 = com.substituemanagment.managment.ui.screens.ComposableSingletons$SmsProcessScreenKt.INSTANCE
                                kotlin.jvm.functions.Function2 r5 = r1.m6936getLambda6$app_debug()
                                r7 = 196608(0x30000, float:2.75506E-40)
                                r8 = 30
                                r1 = 0
                                r2 = 0
                                r3 = 0
                                r4 = 0
                                r6 = r10
                                androidx.compose.material3.IconButtonKt.IconButton(r0, r1, r2, r3, r4, r5, r6, r7, r8)
                                boolean r0 = androidx.compose.runtime.ComposerKt.isTraceInProgress()
                                if (r0 == 0) goto L79
                                androidx.compose.runtime.ComposerKt.traceEventEnd()
                            L79:
                                return
                            */
                            throw new UnsupportedOperationException("Method not decompiled: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8.AnonymousClass1.invoke(androidx.compose.runtime.Composer, int):void");
                        }
                    }

                    /* JADX INFO: Access modifiers changed from: package-private */
                    /* compiled from: SmsProcessScreen.kt */
                    @Metadata(k = 3, mv = {2, 0, 0}, xi = 48)
                    /* renamed from: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$2  reason: invalid class name */
                    /* loaded from: classes9.dex */
                    public static final class AnonymousClass2 implements Function3<RowScope, Composer, Integer, Unit> {
                        final /* synthetic */ NavController $navController;

                        AnonymousClass2(NavController navController) {
                            this.$navController = navController;
                        }

                        @Override // kotlin.jvm.functions.Function3
                        public /* bridge */ /* synthetic */ Unit invoke(RowScope rowScope, Composer composer, Integer num) {
                            invoke(rowScope, composer, num.intValue());
                            return Unit.INSTANCE;
                        }

                        public final void invoke(RowScope TopAppBar, Composer $composer, int $changed) {
                            Function0 function0;
                            Intrinsics.checkNotNullParameter(TopAppBar, "$this$TopAppBar");
                            ComposerKt.sourceInformation($composer, "C239@10369L173,238@10323L519:SmsProcessScreen.kt#ersv7o");
                            if (($changed & 17) != 16 || !$composer.getSkipping()) {
                                if (ComposerKt.isTraceInProgress()) {
                                    ComposerKt.traceEventStart(-833288916, $changed, -1, "com.substituemanagment.managment.ui.screens.SmsProcessScreen.<anonymous>.<anonymous> (SmsProcessScreen.kt:238)");
                                }
                                $composer.startReplaceGroup(1326881791);
                                ComposerKt.sourceInformation($composer, "CC(remember):SmsProcessScreen.kt#9igjgp");
                                boolean changedInstance = $composer.changedInstance(this.$navController);
                                final NavController navController = this.$navController;
                                Object rememberedValue = $composer.rememberedValue();
                                if (changedInstance || rememberedValue == Composer.Companion.getEmpty()) {
                                    function0 = 
                                    /*  JADX ERROR: Method code generation error
                                        jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0054: CONSTRUCTOR  (r8v0 'function0' kotlin.jvm.functions.Function0) = (r1v1 'navController' androidx.navigation.NavController A[DONT_INLINE]) call: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$2$$ExternalSyntheticLambda0.<init>(androidx.navigation.NavController):void type: CONSTRUCTOR in method: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8.2.invoke(androidx.compose.foundation.layout.RowScope, androidx.compose.runtime.Composer, int):void, file: classes9.dex
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:309)
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:272)
                                        	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:91)
                                        	at jadx.core.dex.nodes.IBlock.generate(IBlock.java:15)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:80)
                                        	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:123)
                                        	at jadx.core.dex.regions.conditions.IfRegion.generate(IfRegion.java:90)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:80)
                                        	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:123)
                                        	at jadx.core.dex.regions.conditions.IfRegion.generate(IfRegion.java:90)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
                                        	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:296)
                                        	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:275)
                                        	at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:377)
                                        	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:306)
                                        	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:272)
                                        	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                                        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1541)
                                        	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                                        	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                                        Caused by: jadx.core.utils.exceptions.JadxRuntimeException: Expected class to be processed at this point, class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$2$$ExternalSyntheticLambda0, state: NOT_LOADED
                                        	at jadx.core.dex.nodes.ClassNode.ensureProcessed(ClassNode.java:302)
                                        	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:769)
                                        	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:718)
                                        	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:417)
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:302)
                                        	... 29 more
                                        */
                                    /*
                                        this = this;
                                        java.lang.String r0 = "$this$TopAppBar"
                                        kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r10, r0)
                                        java.lang.String r0 = "C239@10369L173,238@10323L519:SmsProcessScreen.kt#ersv7o"
                                        androidx.compose.runtime.ComposerKt.sourceInformation(r11, r0)
                                        r0 = r12 & 17
                                        r1 = 16
                                        if (r0 != r1) goto L1b
                                        boolean r0 = r11.getSkipping()
                                        if (r0 != 0) goto L17
                                        goto L1b
                                    L17:
                                        r11.skipToGroupEnd()
                                        goto L7f
                                    L1b:
                                        boolean r0 = androidx.compose.runtime.ComposerKt.isTraceInProgress()
                                        if (r0 == 0) goto L2a
                                        r0 = -1
                                        java.lang.String r1 = "com.substituemanagment.managment.ui.screens.SmsProcessScreen.<anonymous>.<anonymous> (SmsProcessScreen.kt:238)"
                                        r2 = -833288916(0xffffffffce55052c, float:-8.9347149E8)
                                        androidx.compose.runtime.ComposerKt.traceEventStart(r2, r12, r0, r1)
                                    L2a:
                                        r0 = 1326881791(0x4f169bff, float:2.52680576E9)
                                        r11.startReplaceGroup(r0)
                                        java.lang.String r0 = "CC(remember):SmsProcessScreen.kt#9igjgp"
                                        androidx.compose.runtime.ComposerKt.sourceInformation(r11, r0)
                                        androidx.navigation.NavController r0 = r9.$navController
                                        boolean r0 = r11.changedInstance(r0)
                                        androidx.navigation.NavController r1 = r9.$navController
                                        r2 = r11
                                        r3 = 0
                                        java.lang.Object r4 = r2.rememberedValue()
                                        r5 = 0
                                        if (r0 != 0) goto L51
                                        androidx.compose.runtime.Composer$Companion r7 = androidx.compose.runtime.Composer.Companion
                                        java.lang.Object r7 = r7.getEmpty()
                                        if (r4 != r7) goto L4f
                                        goto L51
                                    L4f:
                                        r8 = r4
                                        goto L5c
                                    L51:
                                        r7 = 0
                                        com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$2$$ExternalSyntheticLambda0 r8 = new com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8$2$$ExternalSyntheticLambda0
                                        r8.<init>(r1)
                                        r2.updateRememberedValue(r8)
                                    L5c:
                                        r0 = r8
                                        kotlin.jvm.functions.Function0 r0 = (kotlin.jvm.functions.Function0) r0
                                        r11.endReplaceGroup()
                                        com.substituemanagment.managment.ui.screens.ComposableSingletons$SmsProcessScreenKt r1 = com.substituemanagment.managment.ui.screens.ComposableSingletons$SmsProcessScreenKt.INSTANCE
                                        kotlin.jvm.functions.Function2 r5 = r1.m6937getLambda7$app_debug()
                                        r1 = 0
                                        r2 = 0
                                        r3 = 0
                                        r4 = 0
                                        r7 = 196608(0x30000, float:2.75506E-40)
                                        r8 = 30
                                        r6 = r11
                                        androidx.compose.material3.IconButtonKt.IconButton(r0, r1, r2, r3, r4, r5, r6, r7, r8)
                                        boolean r0 = androidx.compose.runtime.ComposerKt.isTraceInProgress()
                                        if (r0 == 0) goto L7f
                                        androidx.compose.runtime.ComposerKt.traceEventEnd()
                                    L7f:
                                        return
                                    */
                                    throw new UnsupportedOperationException("Method not decompiled: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$8.AnonymousClass2.invoke(androidx.compose.foundation.layout.RowScope, androidx.compose.runtime.Composer, int):void");
                                }

                                /* JADX INFO: Access modifiers changed from: private */
                                public static final Unit invoke$lambda$1$lambda$0(NavController $navController) {
                                    NavController.navigate$default($navController, Screen.SmsSend.INSTANCE.getRoute(), (NavOptions) null, (Navigator.Extras) null, 6, (Object) null);
                                    return Unit.INSTANCE;
                                }
                            }
                        }, $composer2, 54), null, ComposableLambdaKt.rememberComposableLambda(1420469307, true, new Function2<Composer, Integer, Unit>() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$SmsProcessScreen$9
                            @Override // kotlin.jvm.functions.Function2
                            public /* bridge */ /* synthetic */ Unit invoke(Composer composer, Integer num) {
                                invoke(composer, num.intValue());
                                return Unit.INSTANCE;
                            }

                            public final void invoke(Composer $composer6, int $changed2) {
                                ComposerKt.sourceInformation($composer6, "C301@13292L31:SmsProcessScreen.kt#ersv7o");
                                if (($changed2 & 3) == 2 && $composer6.getSkipping()) {
                                    $composer6.skipToGroupEnd();
                                    return;
                                }
                                if (ComposerKt.isTraceInProgress()) {
                                    ComposerKt.traceEventStart(1420469307, $changed2, -1, "com.substituemanagment.managment.ui.screens.SmsProcessScreen.<anonymous> (SmsProcessScreen.kt:301)");
                                }
                                SnackbarHostKt.SnackbarHost(SnackbarHostState.this, null, null, $composer6, 6, 6);
                                if (ComposerKt.isTraceInProgress()) {
                                    ComposerKt.traceEventEnd();
                                }
                            }
                        }, $composer2, 54), ComposableLambdaKt.rememberComposableLambda(1128083482, true, new SmsProcessScreenKt$SmsProcessScreen$10(viewModel6, context2, scope, navController, selectedTeachers4, phoneNumbers3, successMessage$delegate, showSuccessBar$delegate, snackbarHostState3), $composer2, 54), 0, 0L, 0L, null, ComposableLambdaKt.rememberComposableLambda(-1423805870, true, new SmsProcessScreenKt$SmsProcessScreen$11(isLoading$delegate, errorMessage$delegate4, showSuccessBar$delegate, successMessage$delegate, selectedTeachers4, viewModel6, showAutoLoadedInfo$delegate, autoLoadedCount$delegate, phoneNumbers3, editingPhoneNumber2, originalPhoneStatus, currentTeacherForSaving$delegate, currentNumberForSaving$delegate, showSaveNumberDialog$delegate2), $composer2, 54), $composer5, 805334064, 485);
                        $composer3 = $composer5;
                        if (ComposerKt.isTraceInProgress()) {
                            ComposerKt.traceEventEnd();
                        }
                    } else {
                        $composer4.skipToGroupEnd();
                        $composer3 = $composer4;
                    }
                    ScopeUpdateScope endRestartGroup = $composer3.endRestartGroup();
                    if (endRestartGroup != null) {
                        endRestartGroup.updateScope(new Function2() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$$ExternalSyntheticLambda1
                            @Override // kotlin.jvm.functions.Function2
                            public final Object invoke(Object obj17, Object obj18) {
                                Unit SmsProcessScreen$lambda$35;
                                SmsProcessScreen$lambda$35 = SmsProcessScreenKt.SmsProcessScreen$lambda$35(NavController.this, $changed, (Composer) obj17, ((Integer) obj18).intValue());
                                return SmsProcessScreen$lambda$35;
                            }
                        });
                    }
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final boolean SmsProcessScreen$lambda$5(State<Boolean> state) {
                    return ((Boolean) state.getValue()).booleanValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final String SmsProcessScreen$lambda$7(State<String> state) {
                    return (String) state.getValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final boolean SmsProcessScreen$lambda$10(MutableState<Boolean> mutableState) {
                    return mutableState.getValue().booleanValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final void SmsProcessScreen$lambda$11(MutableState<Boolean> mutableState, boolean z) {
                    mutableState.setValue(Boolean.valueOf(z));
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final String SmsProcessScreen$lambda$13(MutableState<String> mutableState) {
                    return mutableState.getValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final boolean SmsProcessScreen$lambda$16(MutableState<Boolean> mutableState) {
                    return mutableState.getValue().booleanValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final void SmsProcessScreen$lambda$17(MutableState<Boolean> mutableState, boolean z) {
                    mutableState.setValue(Boolean.valueOf(z));
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final int SmsProcessScreen$lambda$19(MutableState<Integer> mutableState) {
                    return mutableState.getValue().intValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final void SmsProcessScreen$lambda$20(MutableState<Integer> mutableState, int i) {
                    mutableState.setValue(Integer.valueOf(i));
                }

                private static final boolean SmsProcessScreen$lambda$22(MutableState<Boolean> mutableState) {
                    return mutableState.getValue().booleanValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final void SmsProcessScreen$lambda$23(MutableState<Boolean> mutableState, boolean z) {
                    mutableState.setValue(Boolean.valueOf(z));
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final String SmsProcessScreen$lambda$25(MutableState<String> mutableState) {
                    return mutableState.getValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final String SmsProcessScreen$lambda$28(MutableState<String> mutableState) {
                    return mutableState.getValue();
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final void SmsProcessScreen$showSuccess(CoroutineScope scope, MutableState<String> mutableState, MutableState<Boolean> mutableState2, String message) {
                    mutableState.setValue(message);
                    SmsProcessScreen$lambda$11(mutableState2, true);
                    BuildersKt__Builders_commonKt.launch$default(scope, null, null, new SmsProcessScreenKt$SmsProcessScreen$showSuccess$1(mutableState2, null), 3, null);
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final Unit SmsProcessScreen$lambda$34$lambda$33(MutableState $showSaveNumberDialog$delegate) {
                    SmsProcessScreen$lambda$23($showSaveNumberDialog$delegate, false);
                    return Unit.INSTANCE;
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final String getSummaryForAssignments(List<SmsViewModel.AssignmentData> list) {
                    if (list.isEmpty()) {
                        return "No assignments";
                    }
                    List sortedAssignments = CollectionsKt.sortedWith(list, new Comparator() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$getSummaryForAssignments$$inlined$sortedBy$1
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
                    return "Multiple assignments (" + size + "): " + CollectionsKt.joinToString$default(sortedAssignments, ", ", null, null, 0, null, new Function1() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$$ExternalSyntheticLambda2
                        @Override // kotlin.jvm.functions.Function1
                        public final Object invoke(Object obj) {
                            CharSequence summaryForAssignments$lambda$37;
                            summaryForAssignments$lambda$37 = SmsProcessScreenKt.getSummaryForAssignments$lambda$37((SmsViewModel.AssignmentData) obj);
                            return summaryForAssignments$lambda$37;
                        }
                    }, 30, null);
                }

                /* JADX INFO: Access modifiers changed from: private */
                public static final CharSequence getSummaryForAssignments$lambda$37(SmsViewModel.AssignmentData it) {
                    Intrinsics.checkNotNullParameter(it, "it");
                    return "P" + it.getPeriod();
                }

                public static final void TeacherPhoneVerificationItem(final String teacherName, final String phoneNumber, final boolean isEditing, final Function0<Unit> onEditClick, final Function1<? super String, Unit> onPhoneChange, final Function1<? super String, Unit> onDone, final boolean isValid, String assignmentsSummary, boolean isAutoLoaded, boolean hadValidPhoneInitially, Composer $composer, final int $changed, final int i) {
                    boolean z;
                    Object obj;
                    boolean z2;
                    int i2;
                    Composer $composer2;
                    final String assignmentsSummary2;
                    final boolean isAutoLoaded2;
                    final boolean hadValidPhoneInitially2;
                    Intrinsics.checkNotNullParameter(teacherName, "teacherName");
                    Intrinsics.checkNotNullParameter(phoneNumber, "phoneNumber");
                    Intrinsics.checkNotNullParameter(onEditClick, "onEditClick");
                    Intrinsics.checkNotNullParameter(onPhoneChange, "onPhoneChange");
                    Intrinsics.checkNotNullParameter(onDone, "onDone");
                    Composer $composer3 = $composer.startRestartGroup(230278373);
                    ComposerKt.sourceInformation($composer3, "C(TeacherPhoneVerificationItem)P(9,8,3,6,7,5,4!1,2)532@23996L38,533@24041L5855,528@23862L6034:SmsProcessScreen.kt#ersv7o");
                    int $dirty = $changed;
                    if ((i & 1) != 0) {
                        $dirty |= 6;
                    } else if (($changed & 6) == 0) {
                        $dirty |= $composer3.changed(teacherName) ? 4 : 2;
                    }
                    if ((i & 2) != 0) {
                        $dirty |= 48;
                    } else if (($changed & 48) == 0) {
                        $dirty |= $composer3.changed(phoneNumber) ? 32 : 16;
                    }
                    if ((i & 4) != 0) {
                        $dirty |= 384;
                    } else if (($changed & 384) == 0) {
                        $dirty |= $composer3.changed(isEditing) ? 256 : 128;
                    }
                    if ((i & 8) != 0) {
                        $dirty |= 3072;
                    } else if (($changed & 3072) == 0) {
                        $dirty |= $composer3.changedInstance(onEditClick) ? 2048 : 1024;
                    }
                    if ((i & 16) != 0) {
                        $dirty |= 24576;
                    } else if (($changed & 24576) == 0) {
                        $dirty |= $composer3.changedInstance(onPhoneChange) ? 16384 : 8192;
                    }
                    if ((i & 32) != 0) {
                        $dirty |= ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE;
                    } else if (($changed & ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE) == 0) {
                        $dirty |= $composer3.changedInstance(onDone) ? 131072 : 65536;
                    }
                    if ((i & 64) != 0) {
                        $dirty |= 1572864;
                        z = isValid;
                    } else if (($changed & 1572864) == 0) {
                        z = isValid;
                        $dirty |= $composer3.changed(z) ? 1048576 : 524288;
                    } else {
                        z = isValid;
                    }
                    int i3 = i & 128;
                    if (i3 != 0) {
                        $dirty |= 12582912;
                        obj = assignmentsSummary;
                    } else if ((12582912 & $changed) == 0) {
                        obj = assignmentsSummary;
                        $dirty |= $composer3.changed(obj) ? 8388608 : 4194304;
                    } else {
                        obj = assignmentsSummary;
                    }
                    int i4 = i & 256;
                    if (i4 != 0) {
                        $dirty |= 100663296;
                        z2 = isAutoLoaded;
                    } else if ((100663296 & $changed) == 0) {
                        z2 = isAutoLoaded;
                        $dirty |= $composer3.changed(z2) ? AccessibilityEventCompat.TYPE_VIEW_TARGETED_BY_SCROLL : 33554432;
                    } else {
                        z2 = isAutoLoaded;
                    }
                    int i5 = i & 512;
                    if (i5 != 0) {
                        $dirty |= 805306368;
                        i2 = i5;
                    } else if (($changed & 805306368) == 0) {
                        i2 = i5;
                        $dirty |= $composer3.changed(hadValidPhoneInitially) ? 536870912 : 268435456;
                    } else {
                        i2 = i5;
                    }
                    if (($dirty & 306783379) == 306783378 && $composer3.getSkipping()) {
                        $composer3.skipToGroupEnd();
                        hadValidPhoneInitially2 = hadValidPhoneInitially;
                        $composer2 = $composer3;
                        assignmentsSummary2 = obj;
                        isAutoLoaded2 = z2;
                    } else {
                        String assignmentsSummary3 = i3 != 0 ? "" : obj;
                        boolean isAutoLoaded3 = i4 != 0 ? false : z2;
                        boolean hadValidPhoneInitially3 = i2 != 0 ? false : hadValidPhoneInitially;
                        if (ComposerKt.isTraceInProgress()) {
                            ComposerKt.traceEventStart(230278373, $dirty, -1, "com.substituemanagment.managment.ui.screens.TeacherPhoneVerificationItem (SmsProcessScreen.kt:527)");
                        }
                        String assignmentsSummary4 = assignmentsSummary3;
                        Modifier m680paddingVpY3zN4$default = PaddingKt.m680paddingVpY3zN4$default(SizeKt.fillMaxWidth$default(Modifier.Companion, 0.0f, 1, null), 0.0f, Dp.m6513constructorimpl(4), 1, null);
                        CardElevation m1833cardElevationaqJV_2Y = CardDefaults.INSTANCE.m1833cardElevationaqJV_2Y(Dp.m6513constructorimpl(2), 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, $composer3, (CardDefaults.$stable << 18) | 6, 62);
                        boolean isAutoLoaded4 = z;
                        boolean isAutoLoaded5 = isAutoLoaded3;
                        boolean hadValidPhoneInitially4 = hadValidPhoneInitially3;
                        CardKt.Card(m680paddingVpY3zN4$default, null, null, m1833cardElevationaqJV_2Y, null, ComposableLambdaKt.rememberComposableLambda(1659743831, true, new SmsProcessScreenKt$TeacherPhoneVerificationItem$1(isEditing, isAutoLoaded4, onEditClick, teacherName, assignmentsSummary4, phoneNumber, onPhoneChange, onDone, isAutoLoaded5, hadValidPhoneInitially3), $composer3, 54), $composer3, 196614, 22);
                        $composer2 = $composer3;
                        if (ComposerKt.isTraceInProgress()) {
                            ComposerKt.traceEventEnd();
                        }
                        assignmentsSummary2 = assignmentsSummary4;
                        isAutoLoaded2 = isAutoLoaded5;
                        hadValidPhoneInitially2 = hadValidPhoneInitially4;
                    }
                    ScopeUpdateScope endRestartGroup = $composer2.endRestartGroup();
                    if (endRestartGroup != null) {
                        endRestartGroup.updateScope(new Function2() { // from class: com.substituemanagment.managment.ui.screens.SmsProcessScreenKt$$ExternalSyntheticLambda3
                            @Override // kotlin.jvm.functions.Function2
                            public final Object invoke(Object obj2, Object obj3) {
                                Unit TeacherPhoneVerificationItem$lambda$38;
                                TeacherPhoneVerificationItem$lambda$38 = SmsProcessScreenKt.TeacherPhoneVerificationItem$lambda$38(teacherName, phoneNumber, isEditing, onEditClick, onPhoneChange, onDone, isValid, assignmentsSummary2, isAutoLoaded2, hadValidPhoneInitially2, $changed, i, (Composer) obj2, ((Integer) obj3).intValue());
                                return TeacherPhoneVerificationItem$lambda$38;
                            }
                        });
                    }
                }

                public static final boolean isValidPhoneNumber(String phone) {
                    Intrinsics.checkNotNullParameter(phone, "phone");
                    if (StringsKt.isBlank(phone)) {
                        return false;
                    }
                    Regex phoneRegex = new Regex("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$");
                    return phoneRegex.matches(phone);
                }
            }

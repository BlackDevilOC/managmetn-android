package com.substituemanagment.managment.ui.viewmodel;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.autofill.HintConstants;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.SnapshotStateKt;
import androidx.compose.runtime.SnapshotStateKt__SnapshotStateKt;
import androidx.compose.runtime.State;
import androidx.compose.runtime.snapshots.SnapshotStateList;
import androidx.compose.runtime.snapshots.SnapshotStateMap;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelKt;
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.CharsKt;
import kotlin.text.Regex;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.BuildersKt__Builders_commonKt;
import kotlinx.coroutines.Dispatchers;
/* compiled from: SmsViewModel.kt */
@Metadata(d1 = {"\u0000t\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0011\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0007\u0018\u0000 P2\u00020\u0001:\u0004MNOPB\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0006\u0010-\u001a\u00020.J\u0014\u0010/\u001a\b\u0012\u0004\u0012\u00020\n0\u000fH\u0082@¢\u0006\u0002\u00100J\u000e\u00101\u001a\u00020.2\u0006\u00102\u001a\u00020\u0007J\u000e\u00103\u001a\u00020.2\u0006\u00104\u001a\u00020\u0013J\u0006\u00105\u001a\u00020.J\b\u00106\u001a\u00020.H\u0002J\u0006\u00107\u001a\u00020.J\u000e\u00108\u001a\u00020\u00072\u0006\u00109\u001a\u00020\u0016J\u0016\u0010:\u001a\u00020.2\u0006\u00102\u001a\u00020\u00072\u0006\u0010;\u001a\u00020\u0007J\u0006\u0010<\u001a\u00020\u0013J\f\u0010=\u001a\b\u0012\u0004\u0012\u00020\u00070\u000fJ\u001c\u0010>\u001a\u00020.2\u0006\u0010?\u001a\u00020@2\f\u0010A\u001a\b\u0012\u0004\u0012\u00020.0BJ\u0006\u0010C\u001a\u00020\u0013J\u000e\u0010D\u001a\u00020.2\u0006\u0010E\u001a\u00020FJ\f\u0010G\u001a\u00020\u0007*\u00020\u0007H\u0002J\u000e\u0010H\u001a\u00020\u00132\u0006\u0010I\u001a\u00020\u0007J\u0006\u0010J\u001a\u00020.J\u0010\u0010K\u001a\u00020\u00072\u0006\u0010L\u001a\u00020\u0007H\u0002R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082D¢\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR#\u0010\r\u001a\u0014\u0012\u0004\u0012\u00020\u0007\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u000f0\u000e¢\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u001d\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00130\u000e¢\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0011R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\t¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\fR\u001d\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e¢\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0011R\u0014\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00130\u001bX\u0082\u0004¢\u0006\u0002\n\u0000R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00130\u001d¢\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001eR\u0016\u0010\u001f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u001bX\u0082\u0004¢\u0006\u0002\n\u0000R\u0019\u0010 \u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u001d¢\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001eR\u0014\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00130\u001bX\u0082\u0004¢\u0006\u0002\n\u0000R\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00130\u001d¢\u0006\b\n\u0000\u001a\u0004\b$\u0010\u001eR\u0017\u0010%\u001a\b\u0012\u0004\u0012\u00020&0\t¢\u0006\b\n\u0000\u001a\u0004\b'\u0010\fR\u0017\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00070\u001b¢\u0006\b\n\u0000\u001a\u0004\b)\u0010*R\u001d\u0010+\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e¢\u0006\b\n\u0000\u001a\u0004\b,\u0010\u0011¨\u0006Q"}, d2 = {"Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "application", "Landroid/app/Application;", "<init>", "(Landroid/app/Application;)V", "TAG", "", "assignments", "Landroidx/compose/runtime/snapshots/SnapshotStateList;", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$AssignmentData;", "getAssignments", "()Landroidx/compose/runtime/snapshots/SnapshotStateList;", "teacherAssignments", "Landroidx/compose/runtime/snapshots/SnapshotStateMap;", "", "getTeacherAssignments", "()Landroidx/compose/runtime/snapshots/SnapshotStateMap;", "selectedTeachers", "", "getSelectedTeachers", "teachersToProcess", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$TeacherWithAssignments;", "getTeachersToProcess", "phoneNumbers", "getPhoneNumbers", "_isLoading", "Landroidx/compose/runtime/MutableState;", "isLoading", "Landroidx/compose/runtime/State;", "()Landroidx/compose/runtime/State;", "_errorMessage", "errorMessage", "getErrorMessage", "_needsPermission", "needsPermission", "getNeedsPermission", "smsHistory", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$SmsMessage;", "getSmsHistory", "messageTemplate", "getMessageTemplate", "()Landroidx/compose/runtime/MutableState;", "phoneNumberSources", "getPhoneNumberSources", "refreshData", "", "loadAssignmentsFromFile", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toggleTeacherSelection", "teacherName", "selectAllTeachers", "selected", "prepareSelectedTeachersForSms", "saveSelectedTeachersToFile", "loadSelectedTeachersFromFile", "generateSmsForTeacher", "teacher", "updatePhoneNumber", HintConstants.AUTOFILL_HINT_PHONE_NUMBER, "checkAllPhoneNumbersValid", "getTeachersWithMissingPhoneNumbers", "sendSmsToTeachersOneByOne", "context", "Landroid/content/Context;", "onAllSent", "Lkotlin/Function0;", "checkSmsPermissions", "requestSmsPermissions", "activity", "Landroid/app/Activity;", "capitalizeWords", "isValidPhoneNumber", HintConstants.AUTOFILL_HINT_PHONE, "loadPhoneNumbers", "normalizeTeacherName", HintConstants.AUTOFILL_HINT_NAME, "AssignmentData", "TeacherWithAssignments", "SmsMessage", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes5.dex */
public final class SmsViewModel extends AndroidViewModel {
    public static final int $stable = 0;
    public static final Companion Companion = new Companion(null);
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;
    private final String TAG;
    private final MutableState<String> _errorMessage;
    private final MutableState<Boolean> _isLoading;
    private final MutableState<Boolean> _needsPermission;
    private final SnapshotStateList<AssignmentData> assignments;
    private final State<String> errorMessage;
    private final State<Boolean> isLoading;
    private final MutableState<String> messageTemplate;
    private final State<Boolean> needsPermission;
    private final SnapshotStateMap<String, String> phoneNumberSources;
    private final SnapshotStateMap<String, String> phoneNumbers;
    private final SnapshotStateMap<String, Boolean> selectedTeachers;
    private final SnapshotStateList<SmsMessage> smsHistory;
    private final SnapshotStateMap<String, List<AssignmentData>> teacherAssignments;
    private final SnapshotStateList<TeacherWithAssignments> teachersToProcess;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public SmsViewModel(Application application) {
        super(application);
        MutableState<Boolean> mutableStateOf$default;
        MutableState<String> mutableStateOf$default2;
        MutableState<Boolean> mutableStateOf$default3;
        MutableState<String> mutableStateOf$default4;
        Intrinsics.checkNotNullParameter(application, "application");
        this.TAG = "SmsViewModel";
        this.assignments = SnapshotStateKt.mutableStateListOf();
        this.teacherAssignments = SnapshotStateKt.mutableStateMapOf();
        this.selectedTeachers = SnapshotStateKt.mutableStateMapOf();
        this.teachersToProcess = SnapshotStateKt.mutableStateListOf();
        this.phoneNumbers = SnapshotStateKt.mutableStateMapOf();
        mutableStateOf$default = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(false, null, 2, null);
        this._isLoading = mutableStateOf$default;
        this.isLoading = this._isLoading;
        mutableStateOf$default2 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(null, null, 2, null);
        this._errorMessage = mutableStateOf$default2;
        this.errorMessage = this._errorMessage;
        mutableStateOf$default3 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default(false, null, 2, null);
        this._needsPermission = mutableStateOf$default3;
        this.needsPermission = this._needsPermission;
        this.smsHistory = SnapshotStateKt.mutableStateListOf();
        mutableStateOf$default4 = SnapshotStateKt__SnapshotStateKt.mutableStateOf$default("Dear {substitute}, you have been assigned to cover {class} Period {period} on {date} (" + Companion.getCurrentDay() + "). Please confirm your availability.", null, 2, null);
        this.messageTemplate = mutableStateOf$default4;
        this.phoneNumberSources = SnapshotStateKt.mutableStateMapOf();
        refreshData();
    }

    public final SnapshotStateList<AssignmentData> getAssignments() {
        return this.assignments;
    }

    public final SnapshotStateMap<String, List<AssignmentData>> getTeacherAssignments() {
        return this.teacherAssignments;
    }

    public final SnapshotStateMap<String, Boolean> getSelectedTeachers() {
        return this.selectedTeachers;
    }

    public final SnapshotStateList<TeacherWithAssignments> getTeachersToProcess() {
        return this.teachersToProcess;
    }

    public final SnapshotStateMap<String, String> getPhoneNumbers() {
        return this.phoneNumbers;
    }

    public final State<Boolean> isLoading() {
        return this.isLoading;
    }

    public final State<String> getErrorMessage() {
        return this.errorMessage;
    }

    public final State<Boolean> getNeedsPermission() {
        return this.needsPermission;
    }

    public final SnapshotStateList<SmsMessage> getSmsHistory() {
        return this.smsHistory;
    }

    public final MutableState<String> getMessageTemplate() {
        return this.messageTemplate;
    }

    public final SnapshotStateMap<String, String> getPhoneNumberSources() {
        return this.phoneNumberSources;
    }

    /* compiled from: SmsViewModel.kt */
    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0014\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001B9\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0003\u0012\b\b\u0002\u0010\t\u001a\u00020\u0003¢\u0006\u0004\b\n\u0010\u000bJ\t\u0010\u0014\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0015\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0016\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0017\u001a\u00020\u0007HÆ\u0003J\t\u0010\u0018\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0019\u001a\u00020\u0003HÆ\u0003JE\u0010\u001a\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00032\b\b\u0002\u0010\t\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001e\u001a\u00020\u0007HÖ\u0001J\t\u0010\u001f\u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0007¢\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\b\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\rR\u0011\u0010\t\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\r¨\u0006 "}, d2 = {"Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$AssignmentData;", "", "originalTeacher", "", "substitute", "substitutePhone", "period", "", "className", "date", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V", "getOriginalTeacher", "()Ljava/lang/String;", "getSubstitute", "getSubstitutePhone", "getPeriod", "()I", "getClassName", "getDate", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    /* loaded from: classes5.dex */
    public static final class AssignmentData {
        public static final int $stable = 0;
        private final String className;
        private final String date;
        private final String originalTeacher;
        private final int period;
        private final String substitute;
        private final String substitutePhone;

        public static /* synthetic */ AssignmentData copy$default(AssignmentData assignmentData, String str, String str2, String str3, int i, String str4, String str5, int i2, Object obj) {
            if ((i2 & 1) != 0) {
                str = assignmentData.originalTeacher;
            }
            if ((i2 & 2) != 0) {
                str2 = assignmentData.substitute;
            }
            if ((i2 & 4) != 0) {
                str3 = assignmentData.substitutePhone;
            }
            if ((i2 & 8) != 0) {
                i = assignmentData.period;
            }
            if ((i2 & 16) != 0) {
                str4 = assignmentData.className;
            }
            if ((i2 & 32) != 0) {
                str5 = assignmentData.date;
            }
            String str6 = str4;
            String str7 = str5;
            return assignmentData.copy(str, str2, str3, i, str6, str7);
        }

        public final String component1() {
            return this.originalTeacher;
        }

        public final String component2() {
            return this.substitute;
        }

        public final String component3() {
            return this.substitutePhone;
        }

        public final int component4() {
            return this.period;
        }

        public final String component5() {
            return this.className;
        }

        public final String component6() {
            return this.date;
        }

        public final AssignmentData copy(String originalTeacher, String substitute, String substitutePhone, int i, String className, String date) {
            Intrinsics.checkNotNullParameter(originalTeacher, "originalTeacher");
            Intrinsics.checkNotNullParameter(substitute, "substitute");
            Intrinsics.checkNotNullParameter(substitutePhone, "substitutePhone");
            Intrinsics.checkNotNullParameter(className, "className");
            Intrinsics.checkNotNullParameter(date, "date");
            return new AssignmentData(originalTeacher, substitute, substitutePhone, i, className, date);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof AssignmentData) {
                AssignmentData assignmentData = (AssignmentData) obj;
                return Intrinsics.areEqual(this.originalTeacher, assignmentData.originalTeacher) && Intrinsics.areEqual(this.substitute, assignmentData.substitute) && Intrinsics.areEqual(this.substitutePhone, assignmentData.substitutePhone) && this.period == assignmentData.period && Intrinsics.areEqual(this.className, assignmentData.className) && Intrinsics.areEqual(this.date, assignmentData.date);
            }
            return false;
        }

        public int hashCode() {
            return (((((((((this.originalTeacher.hashCode() * 31) + this.substitute.hashCode()) * 31) + this.substitutePhone.hashCode()) * 31) + Integer.hashCode(this.period)) * 31) + this.className.hashCode()) * 31) + this.date.hashCode();
        }

        public String toString() {
            String str = this.originalTeacher;
            String str2 = this.substitute;
            String str3 = this.substitutePhone;
            int i = this.period;
            String str4 = this.className;
            return "AssignmentData(originalTeacher=" + str + ", substitute=" + str2 + ", substitutePhone=" + str3 + ", period=" + i + ", className=" + str4 + ", date=" + this.date + ")";
        }

        public AssignmentData(String originalTeacher, String substitute, String substitutePhone, int period, String className, String date) {
            Intrinsics.checkNotNullParameter(originalTeacher, "originalTeacher");
            Intrinsics.checkNotNullParameter(substitute, "substitute");
            Intrinsics.checkNotNullParameter(substitutePhone, "substitutePhone");
            Intrinsics.checkNotNullParameter(className, "className");
            Intrinsics.checkNotNullParameter(date, "date");
            this.originalTeacher = originalTeacher;
            this.substitute = substitute;
            this.substitutePhone = substitutePhone;
            this.period = period;
            this.className = className;
            this.date = date;
        }

        /* JADX WARN: Illegal instructions before constructor call */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public /* synthetic */ AssignmentData(java.lang.String r8, java.lang.String r9, java.lang.String r10, int r11, java.lang.String r12, java.lang.String r13, int r14, kotlin.jvm.internal.DefaultConstructorMarker r15) {
            /*
                r7 = this;
                r14 = r14 & 32
                if (r14 == 0) goto Lc
                com.substituemanagment.managment.ui.viewmodel.SmsViewModel$Companion r13 = com.substituemanagment.managment.ui.viewmodel.SmsViewModel.Companion
                java.lang.String r13 = r13.getCurrentDate()
                r6 = r13
                goto Ld
            Lc:
                r6 = r13
            Ld:
                r0 = r7
                r1 = r8
                r2 = r9
                r3 = r10
                r4 = r11
                r5 = r12
                r0.<init>(r1, r2, r3, r4, r5, r6)
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.substituemanagment.managment.ui.viewmodel.SmsViewModel.AssignmentData.<init>(java.lang.String, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, int, kotlin.jvm.internal.DefaultConstructorMarker):void");
        }

        public final String getOriginalTeacher() {
            return this.originalTeacher;
        }

        public final String getSubstitute() {
            return this.substitute;
        }

        public final String getSubstitutePhone() {
            return this.substitutePhone;
        }

        public final int getPeriod() {
            return this.period;
        }

        public final String getClassName() {
            return this.className;
        }

        public final String getDate() {
            return this.date;
        }
    }

    /* compiled from: SmsViewModel.kt */
    @Metadata(d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006¢\u0006\u0004\b\b\u0010\tJ\t\u0010\u000f\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0010\u001a\u00020\u0003HÆ\u0003J\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006HÆ\u0003J-\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006HÆ\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0016\u001a\u00020\u0017HÖ\u0001J\t\u0010\u0018\u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000bR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e¨\u0006\u0019"}, d2 = {"Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$TeacherWithAssignments;", "", HintConstants.AUTOFILL_HINT_NAME, "", HintConstants.AUTOFILL_HINT_PHONE, "assignments", "", "Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$AssignmentData;", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)V", "getName", "()Ljava/lang/String;", "getPhone", "getAssignments", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    /* loaded from: classes5.dex */
    public static final class TeacherWithAssignments {
        public static final int $stable = 8;
        private final List<AssignmentData> assignments;
        private final String name;
        private final String phone;

        /* JADX WARN: Multi-variable type inference failed */
        public static /* synthetic */ TeacherWithAssignments copy$default(TeacherWithAssignments teacherWithAssignments, String str, String str2, List list, int i, Object obj) {
            if ((i & 1) != 0) {
                str = teacherWithAssignments.name;
            }
            if ((i & 2) != 0) {
                str2 = teacherWithAssignments.phone;
            }
            if ((i & 4) != 0) {
                list = teacherWithAssignments.assignments;
            }
            return teacherWithAssignments.copy(str, str2, list);
        }

        public final String component1() {
            return this.name;
        }

        public final String component2() {
            return this.phone;
        }

        public final List<AssignmentData> component3() {
            return this.assignments;
        }

        public final TeacherWithAssignments copy(String name, String phone, List<AssignmentData> assignments) {
            Intrinsics.checkNotNullParameter(name, "name");
            Intrinsics.checkNotNullParameter(phone, "phone");
            Intrinsics.checkNotNullParameter(assignments, "assignments");
            return new TeacherWithAssignments(name, phone, assignments);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TeacherWithAssignments) {
                TeacherWithAssignments teacherWithAssignments = (TeacherWithAssignments) obj;
                return Intrinsics.areEqual(this.name, teacherWithAssignments.name) && Intrinsics.areEqual(this.phone, teacherWithAssignments.phone) && Intrinsics.areEqual(this.assignments, teacherWithAssignments.assignments);
            }
            return false;
        }

        public int hashCode() {
            return (((this.name.hashCode() * 31) + this.phone.hashCode()) * 31) + this.assignments.hashCode();
        }

        public String toString() {
            String str = this.name;
            String str2 = this.phone;
            return "TeacherWithAssignments(name=" + str + ", phone=" + str2 + ", assignments=" + this.assignments + ")";
        }

        public TeacherWithAssignments(String name, String phone, List<AssignmentData> assignments) {
            Intrinsics.checkNotNullParameter(name, "name");
            Intrinsics.checkNotNullParameter(phone, "phone");
            Intrinsics.checkNotNullParameter(assignments, "assignments");
            this.name = name;
            this.phone = phone;
            this.assignments = assignments;
        }

        public final String getName() {
            return this.name;
        }

        public final String getPhone() {
            return this.phone;
        }

        public final List<AssignmentData> getAssignments() {
            return this.assignments;
        }
    }

    /* compiled from: SmsViewModel.kt */
    @Metadata(d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\t\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B7\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\u0003¢\u0006\u0004\b\n\u0010\u000bJ\t\u0010\u0014\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0015\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0016\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0017\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0018\u001a\u00020\bHÆ\u0003J\t\u0010\u0019\u001a\u00020\u0003HÆ\u0003JE\u0010\u001a\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001e\u001a\u00020\u001fHÖ\u0001J\t\u0010 \u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\rR\u0011\u0010\u0007\u001a\u00020\b¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\t\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\r¨\u0006!"}, d2 = {"Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$SmsMessage;", "", "id", "", "teacherName", "teacherPhone", "message", "timestamp", "", NotificationCompat.CATEGORY_STATUS, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;)V", "getId", "()Ljava/lang/String;", "getTeacherName", "getTeacherPhone", "getMessage", "getTimestamp", "()J", "getStatus", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    /* loaded from: classes5.dex */
    public static final class SmsMessage {
        public static final int $stable = 0;
        private final String id;
        private final String message;
        private final String status;
        private final String teacherName;
        private final String teacherPhone;
        private final long timestamp;

        public static /* synthetic */ SmsMessage copy$default(SmsMessage smsMessage, String str, String str2, String str3, String str4, long j, String str5, int i, Object obj) {
            if ((i & 1) != 0) {
                str = smsMessage.id;
            }
            if ((i & 2) != 0) {
                str2 = smsMessage.teacherName;
            }
            if ((i & 4) != 0) {
                str3 = smsMessage.teacherPhone;
            }
            if ((i & 8) != 0) {
                str4 = smsMessage.message;
            }
            if ((i & 16) != 0) {
                j = smsMessage.timestamp;
            }
            if ((i & 32) != 0) {
                str5 = smsMessage.status;
            }
            String str6 = str5;
            long j2 = j;
            return smsMessage.copy(str, str2, str3, str4, j2, str6);
        }

        public final String component1() {
            return this.id;
        }

        public final String component2() {
            return this.teacherName;
        }

        public final String component3() {
            return this.teacherPhone;
        }

        public final String component4() {
            return this.message;
        }

        public final long component5() {
            return this.timestamp;
        }

        public final String component6() {
            return this.status;
        }

        public final SmsMessage copy(String id, String teacherName, String teacherPhone, String message, long j, String status) {
            Intrinsics.checkNotNullParameter(id, "id");
            Intrinsics.checkNotNullParameter(teacherName, "teacherName");
            Intrinsics.checkNotNullParameter(teacherPhone, "teacherPhone");
            Intrinsics.checkNotNullParameter(message, "message");
            Intrinsics.checkNotNullParameter(status, "status");
            return new SmsMessage(id, teacherName, teacherPhone, message, j, status);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof SmsMessage) {
                SmsMessage smsMessage = (SmsMessage) obj;
                return Intrinsics.areEqual(this.id, smsMessage.id) && Intrinsics.areEqual(this.teacherName, smsMessage.teacherName) && Intrinsics.areEqual(this.teacherPhone, smsMessage.teacherPhone) && Intrinsics.areEqual(this.message, smsMessage.message) && this.timestamp == smsMessage.timestamp && Intrinsics.areEqual(this.status, smsMessage.status);
            }
            return false;
        }

        public int hashCode() {
            return (((((((((this.id.hashCode() * 31) + this.teacherName.hashCode()) * 31) + this.teacherPhone.hashCode()) * 31) + this.message.hashCode()) * 31) + Long.hashCode(this.timestamp)) * 31) + this.status.hashCode();
        }

        public String toString() {
            String str = this.id;
            String str2 = this.teacherName;
            String str3 = this.teacherPhone;
            String str4 = this.message;
            long j = this.timestamp;
            return "SmsMessage(id=" + str + ", teacherName=" + str2 + ", teacherPhone=" + str3 + ", message=" + str4 + ", timestamp=" + j + ", status=" + this.status + ")";
        }

        public SmsMessage(String id, String teacherName, String teacherPhone, String message, long timestamp, String status) {
            Intrinsics.checkNotNullParameter(id, "id");
            Intrinsics.checkNotNullParameter(teacherName, "teacherName");
            Intrinsics.checkNotNullParameter(teacherPhone, "teacherPhone");
            Intrinsics.checkNotNullParameter(message, "message");
            Intrinsics.checkNotNullParameter(status, "status");
            this.id = id;
            this.teacherName = teacherName;
            this.teacherPhone = teacherPhone;
            this.message = message;
            this.timestamp = timestamp;
            this.status = status;
        }

        public final String getId() {
            return this.id;
        }

        public final String getTeacherName() {
            return this.teacherName;
        }

        public final String getTeacherPhone() {
            return this.teacherPhone;
        }

        public final String getMessage() {
            return this.message;
        }

        public final long getTimestamp() {
            return this.timestamp;
        }

        public final String getStatus() {
            return this.status;
        }
    }

    public final void refreshData() {
        BuildersKt__Builders_commonKt.launch$default(ViewModelKt.getViewModelScope(this), null, null, new SmsViewModel$refreshData$1(this, null), 3, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final Object loadAssignmentsFromFile(Continuation<? super List<AssignmentData>> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), new SmsViewModel$loadAssignmentsFromFile$2(this, null), continuation);
    }

    public final void toggleTeacherSelection(String teacherName) {
        Intrinsics.checkNotNullParameter(teacherName, "teacherName");
        if (this.selectedTeachers.containsKey(teacherName)) {
            SnapshotStateMap<String, Boolean> snapshotStateMap = this.selectedTeachers;
            Boolean bool = this.selectedTeachers.get(teacherName);
            snapshotStateMap.put(teacherName, Boolean.valueOf(!(bool != null ? bool.booleanValue() : false)));
        }
    }

    public final void selectAllTeachers(boolean selected) {
        for (String str : this.selectedTeachers.keySet()) {
            Boolean valueOf = Boolean.valueOf(selected);
            this.selectedTeachers.put(str, valueOf);
        }
    }

    public final void prepareSelectedTeachersForSms() {
        this.teachersToProcess.clear();
        this.phoneNumbers.clear();
        Map linkedHashMap = new LinkedHashMap();
        for (Map.Entry entry : this.selectedTeachers.entrySet()) {
            if (entry.getValue().booleanValue()) {
                linkedHashMap.put(entry.getKey(), entry.getValue());
            }
        }
        List<String> selectedTeachersList = CollectionsKt.toList(linkedHashMap.keySet());
        Log.d(this.TAG, "Preparing " + selectedTeachersList.size() + " selected teachers for SMS");
        for (String str : selectedTeachersList) {
            List list = this.teacherAssignments.get(str);
            if (list == null) {
                list = CollectionsKt.emptyList();
            }
            if (!list.isEmpty()) {
                String substitutePhone = ((AssignmentData) CollectionsKt.first((List<? extends Object>) list)).getSubstitutePhone();
                this.teachersToProcess.add(new TeacherWithAssignments(str, substitutePhone, list));
                this.phoneNumbers.put(str, substitutePhone);
            }
        }
        saveSelectedTeachersToFile();
        Log.d(this.TAG, "Prepared " + this.teachersToProcess.size() + " teachers for SMS processing");
    }

    private final void saveSelectedTeachersToFile() {
        BuildersKt__Builders_commonKt.launch$default(ViewModelKt.getViewModelScope(this), Dispatchers.getIO(), null, new SmsViewModel$saveSelectedTeachersToFile$1(this, null), 2, null);
    }

    public final void loadSelectedTeachersFromFile() {
        BuildersKt__Builders_commonKt.launch$default(ViewModelKt.getViewModelScope(this), Dispatchers.getIO(), null, new SmsViewModel$loadSelectedTeachersFromFile$1(this, null), 2, null);
    }

    public final String generateSmsForTeacher(TeacherWithAssignments teacher) {
        Intrinsics.checkNotNullParameter(teacher, "teacher");
        if (teacher.getAssignments().isEmpty()) {
            return "";
        }
        List sortedAssignments = CollectionsKt.sortedWith(teacher.getAssignments(), new Comparator() { // from class: com.substituemanagment.managment.ui.viewmodel.SmsViewModel$generateSmsForTeacher$$inlined$sortedBy$1
            @Override // java.util.Comparator
            public final int compare(T t, T t2) {
                return ComparisonsKt.compareValues(Integer.valueOf(((SmsViewModel.AssignmentData) t).getPeriod()), Integer.valueOf(((SmsViewModel.AssignmentData) t2).getPeriod()));
            }
        });
        if (sortedAssignments.size() == 1) {
            AssignmentData assignment = (AssignmentData) CollectionsKt.first((List<? extends Object>) sortedAssignments);
            return StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(this.messageTemplate.getValue(), "{substitute}", teacher.getName(), false, 4, (Object) null), "{class}", assignment.getClassName(), false, 4, (Object) null), "{period}", String.valueOf(assignment.getPeriod()), false, 4, (Object) null), "{date}", assignment.getDate(), false, 4, (Object) null), "{day}", Companion.getCurrentDay(), false, 4, (Object) null), "{original_teacher}", assignment.getOriginalTeacher(), false, 4, (Object) null);
        }
        String baseMessage = "Dear " + teacher.getName() + ", you have been assigned to the following classes on " + ((AssignmentData) CollectionsKt.first((List<? extends Object>) sortedAssignments)).getDate() + " (" + Companion.getCurrentDay() + "):";
        String assignmentsText = CollectionsKt.joinToString$default(sortedAssignments, "\n", null, null, 0, null, new Function1() { // from class: com.substituemanagment.managment.ui.viewmodel.SmsViewModel$$ExternalSyntheticLambda1
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                CharSequence generateSmsForTeacher$lambda$4;
                generateSmsForTeacher$lambda$4 = SmsViewModel.generateSmsForTeacher$lambda$4((SmsViewModel.AssignmentData) obj);
                return generateSmsForTeacher$lambda$4;
            }
        }, 30, null);
        return baseMessage + "\n\n" + assignmentsText + "\n\nPlease confirm your availability.";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence generateSmsForTeacher$lambda$4(AssignmentData assignment) {
        Intrinsics.checkNotNullParameter(assignment, "assignment");
        int period = assignment.getPeriod();
        String className = assignment.getClassName();
        return "• Period " + period + ": " + className + " (for " + assignment.getOriginalTeacher() + ")";
    }

    public final void updatePhoneNumber(String teacherName, String phoneNumber) {
        Intrinsics.checkNotNullParameter(teacherName, "teacherName");
        Intrinsics.checkNotNullParameter(phoneNumber, "phoneNumber");
        this.phoneNumbers.put(teacherName, phoneNumber);
        int index = 0;
        Iterator<TeacherWithAssignments> it = this.teachersToProcess.iterator();
        while (true) {
            if (it.hasNext()) {
                if (Intrinsics.areEqual(((TeacherWithAssignments) it.next()).getName(), teacherName)) {
                    break;
                }
                index++;
            } else {
                index = -1;
                break;
            }
        }
        if (index >= 0) {
            TeacherWithAssignments teacher = this.teachersToProcess.get(index);
            this.teachersToProcess.set(index, TeacherWithAssignments.copy$default(teacher, null, phoneNumber, null, 5, null));
        }
    }

    public final boolean checkAllPhoneNumbersValid() {
        Iterable<TeacherWithAssignments> iterable = this.teachersToProcess;
        if ((iterable instanceof Collection) && ((Collection) iterable).isEmpty()) {
            return true;
        }
        for (TeacherWithAssignments teacherWithAssignments : iterable) {
            String str = this.phoneNumbers.get(teacherWithAssignments.getName());
            if (str == null) {
                str = "";
            }
            if (!isValidPhoneNumber(str)) {
                return false;
            }
        }
        return true;
    }

    public final List<String> getTeachersWithMissingPhoneNumbers() {
        Collection arrayList = new ArrayList();
        for (TeacherWithAssignments teacherWithAssignments : this.teachersToProcess) {
            String str = this.phoneNumbers.get(teacherWithAssignments.getName());
            if (str == null) {
                str = "";
            }
            if (!isValidPhoneNumber(str)) {
                arrayList.add(teacherWithAssignments);
            }
        }
        Iterable<TeacherWithAssignments> iterable = (List) arrayList;
        Collection arrayList2 = new ArrayList(CollectionsKt.collectionSizeOrDefault(iterable, 10));
        for (TeacherWithAssignments teacherWithAssignments2 : iterable) {
            arrayList2.add(teacherWithAssignments2.getName());
        }
        return (List) arrayList2;
    }

    public final void sendSmsToTeachersOneByOne(Context context, Function0<Unit> onAllSent) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(onAllSent, "onAllSent");
        if (this.teachersToProcess.isEmpty()) {
            this._errorMessage.setValue("No teachers selected for SMS");
        } else if (checkSmsPermissions()) {
            BuildersKt__Builders_commonKt.launch$default(ViewModelKt.getViewModelScope(this), null, null, new SmsViewModel$sendSmsToTeachersOneByOne$1(this, context, onAllSent, null), 3, null);
        }
    }

    public final boolean checkSmsPermissions() {
        Application context = getApplication();
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(context, "android.permission.SEND_SMS") == 0;
        this._needsPermission.setValue(Boolean.valueOf(hasSmsPermission ? false : true));
        return hasSmsPermission;
    }

    public final void requestSmsPermissions(Activity activity) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        ActivityCompat.requestPermissions(activity, new String[]{"android.permission.SEND_SMS"}, SMS_PERMISSION_REQUEST_CODE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String capitalizeWords(String $this$capitalizeWords) {
        return CollectionsKt.joinToString$default(StringsKt.split$default((CharSequence) $this$capitalizeWords, new String[]{" "}, false, 0, 6, (Object) null), " ", null, null, 0, null, new Function1() { // from class: com.substituemanagment.managment.ui.viewmodel.SmsViewModel$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                CharSequence capitalizeWords$lambda$10;
                capitalizeWords$lambda$10 = SmsViewModel.capitalizeWords$lambda$10((String) obj);
                return capitalizeWords$lambda$10;
            }
        }, 30, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence capitalizeWords$lambda$10(String word) {
        String str;
        Intrinsics.checkNotNullParameter(word, "word");
        if (word.length() > 0) {
            StringBuilder sb = new StringBuilder();
            char charAt = word.charAt(0);
            StringBuilder append = sb.append((Object) (Character.isLowerCase(charAt) ? CharsKt.titlecase(charAt) : String.valueOf(charAt)));
            String substring = word.substring(1);
            Intrinsics.checkNotNullExpressionValue(substring, "substring(...)");
            str = append.append(substring).toString();
        } else {
            str = word;
        }
        return str;
    }

    public final boolean isValidPhoneNumber(String phone) {
        Intrinsics.checkNotNullParameter(phone, "phone");
        if (StringsKt.isBlank(phone)) {
            return false;
        }
        Regex phoneRegex = new Regex("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$");
        return phoneRegex.matches(phone);
    }

    public final void loadPhoneNumbers() {
        BuildersKt__Builders_commonKt.launch$default(ViewModelKt.getViewModelScope(this), Dispatchers.getIO(), null, new SmsViewModel$loadPhoneNumbers$1(this, null), 2, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String normalizeTeacherName(String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
        Regex regex = new Regex("^(sir|miss|mrs?|ms)\\s+");
        return StringsKt.trim((CharSequence) new Regex("\\s+").replace(regex.replace(lowerCase, ""), " ")).toString();
    }

    /* compiled from: SmsViewModel.kt */
    @Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0006\u001a\u00020\u0007J\u0006\u0010\b\u001a\u00020\u0007R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T¢\u0006\u0002\n\u0000¨\u0006\t"}, d2 = {"Lcom/substituemanagment/managment/ui/viewmodel/SmsViewModel$Companion;", "", "<init>", "()V", "SMS_PERMISSION_REQUEST_CODE", "", "getCurrentDate", "", "getCurrentDay", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    /* loaded from: classes5.dex */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final String getCurrentDate() {
            String format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Intrinsics.checkNotNullExpressionValue(format, "format(...)");
            return format;
        }

        public final String getCurrentDay() {
            String format = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
            Intrinsics.checkNotNullExpressionValue(format, "format(...)");
            return format;
        }
    }
}

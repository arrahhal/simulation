
// استيراد مكتبة لقراءة البيانات من الملفات
// استيراد مكتبة لكتابة البيانات إلى الملفات
// استيراد مكتبة لفتح الملفات للقراءة
// استيراد مكتبة لفتح الملفات للكتابة
import java.io.*; // استيراد كل مكونات الحزمة java.io
import java.nio.file.*; // استيراد مكتبة لإدارة الملفات

public class MM1p2 {

    final int Q_LIMIT = 100; // الحد الأقصى لعدد العملاء في الصف

    // تعريف الحالة الحالية للخادم باستخدام enum
    enum ServerStatus {
        IDLE, // حالة الخمول
        BUSY // حالة الانشغال
    }

    ServerStatus server_status; // حالة الخادم (مشغول أو خامل)

    // مصفوفة تخزن أوقات وصول العملاء إلى الصف
    double[] time_arrival = new double[Q_LIMIT + 1];
    // مصفوفة تخزن توقيتات الأحداث التالية (وصول أو مغادرة)
    double[] time_next_event = new double[3];

    // تعريف المتغيرات الرئيسية للمحاكاة
    int next_event_type, // نوع الحدث التالي (وصول أو مغادرة)
            num_custs_delayed, // عدد العملاء الذين تم تأخيرهم
            num_delays_required, // عدد العملاء المطلوب خدمتهم
            num_events, // عدد الأحداث الممكنة (وصول أو مغادرة)
            num_in_q; // عدد العملاء في الصف حاليًا

    // المتغيرات الإحصائية الخاصة بالمحاكاة
    double area_num_in_q, // المساحة تحت منحنى عدد العملاء في الصف
            area_server_status, // المساحة تحت مؤشر انشغال الخادم
            mean_interarrival, // متوسط وقت الوصول بين العملاء
            mean_service, // متوسط وقت الخدمة لكل عميل
            sim_time, // الوقت الحالي للمحاكاة
            time_last_event, // وقت آخر حدث تم تنفيذه
            total_of_delays; // إجمالي زمن التأخير لكل العملاء

    BufferedReader infile; // متغير لقراءة بيانات الإدخال من ملف
    BufferedWriter outfile; // متغير لكتابة بيانات الإخراج إلى ملف

    // وظيفة لحذف ملف معين
    private void deleteFile(String path) {
        try {
            (new File(path)).delete(); // حذف الملف المحدد
        } catch (Exception x) {
            System.err.format("%s: error in deleting ", path); // طباعة خطأ في حال فشل الحذف
        }
    }

    // وظيفة تشغيل المحاكاة
    public void runSimulation() throws IOException {
        // حذف ملف الإخراج القديم قبل بدء المحاكاة
        deleteFile("data/mm1.out");

        // فتح ملفات الإدخال والإخراج
        infile = new BufferedReader(new FileReader("data/mm1.in"));
        outfile = new BufferedWriter(new FileWriter("data/mm1.out"));

        // تحديد عدد الأحداث الممكنة (وصول أو مغادرة)
        num_events = 2;

        // قراءة المعطيات من ملف الإدخال
        String[] params = infile.readLine().trim().split("\\s+");
        assert params.length == 3; // التأكد من وجود 3 معطيات
        mean_interarrival = Double.valueOf(params[0]); // متوسط وقت الوصول
        mean_service = Double.valueOf(params[1]); // متوسط وقت الخدمة
        num_delays_required = Integer.valueOf(params[2]); // عدد العملاء المطلوب خدمتهم

        // كتابة العنوان والمعطيات إلى ملف الإخراج
        outfile.write("Single-server queueing system\n\n");
        outfile.write("Mean interarrival time " + mean_interarrival + " minutes\n\n");
        outfile.write("Mean service time " + mean_service + " minutes\n\n");
        outfile.write("Number of customers " + num_delays_required + "\n\n");

        // تهيئة المتغيرات وتشغيل المحاكاة
        initialize();

        // تنفيذ المحاكاة حتى يتم خدمة جميع العملاء المطلوبين
        while (num_custs_delayed < num_delays_required) {
            timing(); // تحديد الحدث التالي
            update_time_avg_stats(); // تحديث الإحصائيات
            switch (next_event_type) { // اختيار نوع الحدث
                case 1:
                    arrive();
                    break; // وصول عميل
                case 2:
                    depart();
                    break; // مغادرة عميل
            }
        }

        // كتابة التقرير النهائي وإغلاق الملفات
        report();
        infile.close();
        outfile.close();
    }

    // وظيفة تهيئة المحاكاة
    void initialize() {
        sim_time = 0.0; // تصفير الوقت
        server_status = ServerStatus.IDLE; // جعل الخادم خاملًا
        num_in_q = 0; // تصفير عدد العملاء في الصف
        time_last_event = 0.0; // تصفير وقت آخر حدث
        num_custs_delayed = 0; // تصفير عدد العملاء المؤخرين
        total_of_delays = 0.0; // تصفير مجموع التأخيرات
        area_num_in_q = 0.0; // تصفير المساحة تحت منحنى الصف
        area_server_status = 0.0; // تصفير المساحة تحت منحنى حالة الخادم
        time_next_event[1] = sim_time + expon(mean_interarrival); // جدولة الوصول التالي
        time_next_event[2] = Double.MAX_VALUE; // لا يوجد مغادرة بعد
    }

    // دالة توقيت (timing) لتحديد الحدث التالي في المحاكاة
    void timing() throws IOException {
        // تعيين قيمة أولية كبيرة للعثور على أصغر وقت للحدث القادم.
        double min_time_next_event = Double.MAX_VALUE;

        // تهيئة نوع الحدث التالي إلى 0 (إشارة إلى عدم وجود أحداث محددة بعد).
        next_event_type = 0;

        // حلقة تمر عبر جميع الأحداث لتحديد الحدث صاحب أقرب وقت.
        for (int i = 1; i <= num_events; ++i) {
            // إذا كان وقت الحدث الحالي أصغر من الحد الأدنى المسجل.
            if (time_next_event[i] < min_time_next_event) {
                // تحديث الحد الأدنى وتسجيل نوع الحدث.
                min_time_next_event = time_next_event[i];
                next_event_type = i;
            }
        }

        // التحقق مما إذا لم يتم العثور على أي حدث (next_event_type = 0).
        if (next_event_type == 0) {
            // كتابة رسالة إلى الملف تشير إلى أن قائمة الأحداث فارغة وإنهاء المحاكاة.
            outfile.write("\nEvent list empty at time " + sim_time);
            System.exit(1); // إنهاء المحاكاة لعدم وجود أحداث قادمة.
        }

        // تحديث الوقت الحالي للمحاكاة إلى وقت الحدث التالي.
        sim_time = min_time_next_event;
    }

    // وظيفة وصول عميل جديد
    void arrive() throws IOException {
        // جدولة وصول العميل التالي باستخدام زمن بيني عشوائي (expon) بناءً على متوسط زمن
        // الوصول.
        time_next_event[1] = sim_time + expon(mean_interarrival);

        // التحقق مما إذا كان الخادم مشغولًا.
        if (server_status == ServerStatus.BUSY) {
            // إذا كان مشغولًا، تتم زيادة عدد العملاء في قائمة الانتظار.
            ++num_in_q;

            // التحقق إذا تجاوز عدد العملاء في الصف الحد الأقصى المسموح به.
            if (num_in_q > Q_LIMIT) {
                // تسجيل حالة تجاوز السعة في الملف وإيقاف المحاكاة.
                outfile.write("\nOverflow of the array time_arrival at time " + sim_time);
                System.exit(2); // إنهاء المحاكاة بسبب تجاوز الحد الأقصى.
            }

            // تسجيل وقت وصول العميل إلى الصف.
            time_arrival[num_in_q] = sim_time;
        } else {
            // إذا كان الخادم غير مشغول، تتم زيادة عدد العملاء الذين تأخروا.
            ++num_custs_delayed;

            // تغيير حالة الخادم إلى مشغول (BUSY).
            server_status = ServerStatus.BUSY;

            // جدولة وقت إنهاء خدمة هذا العميل.
            time_next_event[2] = sim_time + expon(mean_service);
        }
    }

    // وظيفة مغادرة عميل
    // دالة المغادرة (depart) تقوم بمعالجة خروج العميل من النظام.
    void depart() {
        // إذا لم يكن هناك عملاء في قائمة الانتظار، يصبح الخادم في حالة الخمول (IDLE).
        if (num_in_q == 0) {
            server_status = ServerStatus.IDLE; // تعيين حالة الخادم إلى خامل.
            time_next_event[2] = Double.MAX_VALUE; // لا يوجد حدث مغادرة قريب، لذلك يتم تعيين وقت الحدث إلى قيمة قصوى.
        } else {
            // إذا كان هناك عملاء في الانتظار، يتم إنقاص عدد العملاء في قائمة الانتظار
            // بمقدار 1.
            --num_in_q;
            // حساب التأخير من وقت وصول العميل إلى وقت الخدمة.
            double delay = sim_time - time_arrival[1];
            total_of_delays += delay; // إضافة التأخير إلى إجمالي التأخيرات.
            ++num_custs_delayed; // زيادة عدد العملاء الذين تأخروا.

            // تحديد وقت حدث المغادرة التالي بناءً على زمن الخدمة العشوائي.
            time_next_event[2] = sim_time + expon(mean_service);

            // تحديث مصفوفة أوقات الوصول بعد خروج العميل الأول.
            for (int i = 1; i <= num_in_q; ++i)
                time_arrival[i] = time_arrival[i + 1]; // نقل أوقات الوصول المتبقية للأمام في المصفوفة.
        }
    }

    // دالة تحديث الإحصائيات المتوسطة الزمنية (update_time_avg_stats)
    void update_time_avg_stats() {
        // حساب الزمن المنقضي منذ آخر حدث.
        double time_since_last_event = sim_time - time_last_event;

        // تحديث وقت آخر حدث بالوقت الحالي للمحاكاة.
        time_last_event = sim_time;

        // تحديث المساحة التراكمية تحت منحنى عدد العملاء في قائمة الانتظار.
        area_num_in_q += num_in_q * time_since_last_event;

        // تحديث المساحة التراكمية لحالة الخادم (مشغول أو خامل) مع الأخذ بعين الاعتبار
        // الزمن.
        area_server_status += server_status.ordinal() * time_since_last_event;
    }

    // وظيفة لإصدار التقرير النهائي
    void report() throws IOException {
        outfile.write("\n\nAverage delay in queue " + (total_of_delays / num_custs_delayed) + " minutes\n\n");
        outfile.write("Average number in queue " + (area_num_in_q / sim_time) + "\n\n");
        outfile.write("Server utilization " + (area_server_status / sim_time) + "\n\n");
        outfile.write("Time simulation ended " + sim_time + " minutes");
    }

    // دالة لتوليد أوقات عشوائية باستخدام التوزيع الأسي
    static double expon(double mean) {
        return -mean * Math.log(Math.random());
    }

    // الدالة الرئيسية لتشغيل البرنامج
    public static void main(String[] args) {
        try {
            new MM1p().runSimulation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

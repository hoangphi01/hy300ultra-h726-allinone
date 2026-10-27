import java.lang.reflect.Method;

/**
 * Sets the Allwinner TvServer "source aspect ratio" (overscan screen mode) for the
 * hardware video path (SOURCE_TYPE_VIDEODEC), which otherwise defaults to FULL and
 * stretches cast/mirror video. Calls the vendor HIDL ITvServer directly via reflection
 * so no vendor stub is needed at compile time (MagcubicOS.apk supplies it at runtime
 * via CLASSPATH).
 *
 * EnumOverScanScreenMode: 0=FULL 1=16X9 2=4X3 3=CINEMA 4=ZOOM 5=AUTO 6=DOT_BY_DOT
 * PQ type 21 = OTHER_TYPE_ASPECT_RATIO.
 *
 * Usage (run via app_process32 with MagcubicOS.apk on CLASSPATH):
 *   SetAspect get        -> print current aspect PQ value (type 21)
 *   SetAspect <0-6>      -> setPQValue(21,<mode>) (saved variant), default 1
 *   SetAspect <0-6> nosave -> setAspectRatioNotSave(<mode>) (runtime only)
 */
public class SetAspect {
    private static final String ITV = "vendor.aw.homlet.tvsystem.tvserver.V1_0.ITvServer";
    private static final int ASPECT_PQ_TYPE = 21;

    public static void main(String[] args) {
        try {
            Class<?> itv = Class.forName(ITV);
            Object svc = getService(itv);
            if (svc == null) {
                System.out.println("FAIL: ITvServer.getService() returned null");
                return;
            }
            System.out.println("ITvServer bound: " + svc.getClass().getName());

            boolean doGet = args.length > 0 && args[0].equalsIgnoreCase("get");
            int mode = 1;
            if (!doGet && args.length > 0) {
                try { mode = Integer.parseInt(args[0]); } catch (NumberFormatException e) { mode = 1; }
            }
            boolean nosave = args.length > 1 && args[1].equalsIgnoreCase("nosave");

            // Read current value first (if getPQValue exists).
            Integer before = tryGetPQ(itv, svc);
            System.out.println("aspect(before) type21 = " + before + "  (0=FULL 1=16X9 2=4X3 3=CINEMA 4=ZOOM 5=AUTO 6=DOTBYDOT)");

            if (doGet) return;

            if (nosave) {
                invokeInt2(itv, svc, "setAspectRatioNotSave", mode, -1, "setAspectRatioNotSave(" + mode + ")", true);
            } else {
                // Primary: saved variant via setPQValue(21, mode).
                Object r = invokeInt2(itv, svc, "setPQValue", ASPECT_PQ_TYPE, mode, "setPQValue(21," + mode + ")", false);
                if (r == null) {
                    // Fallback to the not-save path if setPQValue is unavailable.
                    invokeInt2(itv, svc, "setAspectRatioNotSave", mode, -1, "setAspectRatioNotSave(" + mode + ")", true);
                }
            }

            Integer after = tryGetPQ(itv, svc);
            System.out.println("aspect(after)  type21 = " + after);
            System.out.println("DONE requested mode=" + mode + (nosave ? " (nosave)" : " (saved)"));
        } catch (Throwable t) {
            System.out.println("ERROR: " + t);
            t.printStackTrace(System.out);
        }
    }

    /** Try the common HIDL getService overloads. */
    private static Object getService(Class<?> itv) {
        String[][] sigs = {
            {"getService", "bool"},     // getService(boolean)
            {"getService", "void"},     // getService()
            {"getService", "strbool"},  // getService(String, boolean)
            {"getService", "str"}       // getService(String)
        };
        for (String[] s : sigs) {
            try {
                Method m;
                Object r;
                switch (s[1]) {
                    case "bool":    m = itv.getMethod("getService", boolean.class); r = m.invoke(null, true); break;
                    case "void":    m = itv.getMethod("getService"); r = m.invoke(null); break;
                    case "strbool": m = itv.getMethod("getService", String.class, boolean.class); r = m.invoke(null, "default", true); break;
                    case "str":     m = itv.getMethod("getService", String.class); r = m.invoke(null, "default"); break;
                    default: r = null;
                }
                if (r != null) { System.out.println("getService via " + s[1]); return r; }
            } catch (Throwable ignore) { /* try next */ }
        }
        return null;
    }

    private static Integer tryGetPQ(Class<?> itv, Object svc) {
        try {
            Method m = itv.getMethod("getPQValue", int.class);
            Object r = m.invoke(svc, ASPECT_PQ_TYPE);
            if (r instanceof Integer) return (Integer) r;
        } catch (Throwable ignore) {}
        return null;
    }

    /** Invoke an (int,int) or (int) returning-int HIDL method; print result; return result or null. */
    private static Object invokeInt2(Class<?> itv, Object svc, String name, int a, int b, String label, boolean single) {
        try {
            Method m; Object r;
            if (single) { m = itv.getMethod(name, int.class); r = m.invoke(svc, a); }
            else        { m = itv.getMethod(name, int.class, int.class); r = m.invoke(svc, a, b); }
            System.out.println(label + " -> ret=" + r);
            return r == null ? "void" : r;
        } catch (NoSuchMethodException e) {
            System.out.println(label + " : no such method");
            return null;
        } catch (Throwable t) {
            System.out.println(label + " : ERROR " + t);
            return null;
        }
    }
}

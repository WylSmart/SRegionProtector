package Sergey_Dertan.SRegionProtector.Utils;

import cn.nukkit.Player;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.utils.Config;
import com.google.gson.Gson;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Utils {

    private static final Random RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String emailPattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
    private static final String SALT = "AjzzdaASd341Fdsf";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    private Utils() {
    }

    /*---------------- encryption -----------------*/
    public static String createSalt(int length) {
        StringBuilder returnValue = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            returnValue.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new String(returnValue);
    }

    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while encrypting a string: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public static String encryptString(String password, String salt) {
        byte[] securePassword = hash(password.toCharArray(), salt.getBytes());
        return Base64.getEncoder().encodeToString(securePassword);
    }

    public static String encryptString(String password) {
        return encryptString(password, SALT);
    }

    public static boolean verifyString(String string, String encryptedString, String salt) {
        String newSecurePassword = encryptString(string, salt);
        return newSecurePassword.equalsIgnoreCase(encryptedString);
    }

    public static boolean verifyString(String string, String encryptedString) {
        return verifyString(string, encryptedString, SALT);
    }

    /*---------------- encryption end -------------*/

    @SuppressWarnings("unchecked")
    public static Map<String, Object> httpGetRequestJson(String url) throws IOException {
        java.net.HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Gson gson = new Gson();
        Map<String, Object> map = new HashMap<>();
        map = (Map<String, Object>) gson.fromJson(reader.readLine(), map.getClass());
        return map;
    }

    public static boolean isValidEmailAddress(String email) {
        return Pattern.compile(emailPattern).matcher(email).matches();
    }

    /*---------------- serializers ----------------*/

    public static String serializeStringArray(String[] arr) {
        try (final ByteArrayOutputStream boas = new ByteArrayOutputStream(); final ObjectOutputStream oos = new ObjectOutputStream(boas)) {
            oos.writeObject(arr);
            return Base64.getEncoder().encodeToString(boas.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] deserializeStringArray(String data) {
        try (final ByteArrayInputStream bias = new ByteArrayInputStream(Base64.getDecoder().decode(data)); final ObjectInputStream ois = new ObjectInputStream(bias)) {
            return (String[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serializeBooleanArray(boolean[] arr) {
        String[] strings = new String[arr.length];
        for (int i = 0; i < arr.length; ++i) {
            strings[i] = arr[i] ? "true" : "false";
        }
        return serializeStringArray(strings);
    }

    public static boolean[] deserializeBooleanArray(String str) {
        String[] strings = deserializeStringArray(str);
        boolean[] arr = new boolean[strings.length];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = strings[i].equalsIgnoreCase("true");
        }
        return arr;
    }

    /*---------------- serializers end ------------*/

    /*---------------- resources ------------------*/
    @SuppressWarnings("ConstantConditions")
    public static void copyResource(String fileName, String sourceFolder, String targetFolder, Class clazz, boolean fixMissingContents, boolean removeAbsent) throws IOException {
        if (sourceFolder.charAt(sourceFolder.length() - 1) != '/') sourceFolder += '/';
        if (targetFolder.charAt(targetFolder.length() - 1) != '/') targetFolder += '/';
        File file = new File(targetFolder + fileName);
        if (!file.exists()) {
            cn.nukkit.utils.Utils.writeFile(file, clazz.getClassLoader().getResourceAsStream(sourceFolder + fileName));
            return;
        }
        if (!fixMissingContents) return;
        Config var3 = new Config(file.getAbsolutePath(), Config.YAML);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        @SuppressWarnings("unchecked")
        Map<Object, Object> var1 = yaml.loadAs(clazz.getClassLoader().getResourceAsStream(sourceFolder + fileName), HashMap.class);

        Map<String, Object> var4 = var3.getAll();
        @SuppressWarnings("unchecked")
        boolean changed = copyMapOfMaps(var1, (Map) var4, removeAbsent);
        if (changed) {
            LinkedHashMap<String, Object> var5 = new LinkedHashMap<>();
            var4.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(x -> var5.put(x.getKey(), x.getValue()));
            var3.setAll(var5);
            var3.save();
        }
    }

    public static boolean copyMapOfMaps(Map<Object, Object> source, Map<Object, Object> target) {
        return copyMapOfMaps(source, target, true);
    }

    /**
     * recursive map copy
     *
     * @param removeAbsent if true will remove items which src doesn`t contain
     * @return true if target map was changed
     */
    public static boolean copyMapOfMaps(Map<Object, Object> source, Map<Object, Object> target, boolean removeAbsent) {
        boolean changed = false;
        for (Map.Entry it : source.entrySet()) {
            if (!target.containsKey(it.getKey())) {
                changed = true;
                target.put(it.getKey(), it.getValue());
            } else {
                if (!it.getValue().getClass().isAssignableFrom(target.get(it.getKey()).getClass())) {
                    if (it.getValue() instanceof Number) continue;
                    changed = true;
                    target.put(it.getKey(), it.getValue());
                    continue;
                }
                if (it.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    boolean c = copyMapOfMaps((Map<Object, Object>) it.getValue(), (Map<Object, Object>) target.get(it.getKey()), removeAbsent);
                    if (c) changed = true;
                }
            }
        }
        if (removeAbsent) {
            boolean c = target.entrySet().removeIf(e -> !source.containsKey(e.getKey()));
            if (c) changed = true;
        }
        return changed;
    }

    public static void copyResource(String fileName, String sourceFolder, String targetFolder, Class clazz) throws IOException {
        copyResource(fileName, sourceFolder, targetFolder, clazz, true, true);
    }

    public static boolean resourceExists(String fileName, String folder, Class clazz) {
        if (!folder.endsWith("/")) folder += '/';
        return clazz.getClassLoader().getResource(folder + fileName) != null;
    }

    /*---------------- resources end --------------*/

    @SuppressWarnings("unchecked")
    public static <T extends Cloneable> Collection<T> deepClone(Collection<T> arr) {
        Collection<T> copy = new ArrayList<>();
        for (T elem : arr) {
            copy.add((T) elem.clone());
        }
        return copy;
    }

    public static double round(double value, int precision) {
        for (int i = 0; i < precision; ++i) {
            value *= 10D;
        }
        value = Math.round(value);
        for (int i = 0; i < precision; ++i) {
            value /= 10D;
        }
        return value;
    }

    //slices array into pieces with the same size
    public static <T> List<List<T>> sliceArray(T[] array, int pieces, boolean keepEmpty) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < pieces; ++i) {
            result.add(new ArrayList<>());
        }

        int i = 0;

        for (T obj : array) {
            if (i == pieces) i = 0;
            result.get(i).add(obj);
            ++i;
        }
        if (!keepEmpty) {
            result.removeIf(List::isEmpty);
        }
        return result;
    }

    /**
     * @return greater version string or empty string if they are equal
     */
    public static String compareVersions(String first, String second) {
        if (first.equals(second)) return "";
        String[] f = first.split("\\.");
        String[] s = second.split("\\.");

        String[] bigger = f.length >= s.length ? f : s;
        String[] smaller = f.length < s.length ? f : s;

        for (int i = 0; i < smaller.length; ++i) {
            if (Integer.parseInt(smaller[i]) > Integer.parseInt(bigger[i])) {
                return String.join(".", smaller);
            } else if (Integer.parseInt(smaller[i]) < Integer.parseInt(bigger[i])) {
                return String.join(".", bigger);
            }
            if (smaller.length == i + 1 && smaller.length < bigger.length) return String.join(".", bigger);
        }
        throw new RuntimeException("Unreachable code reached");
    }

    public static boolean directDataPacket(Player target, DataPacket pk) {
        try {
            Field interfaz = Player.class.getDeclaredField("interfaz");

            boolean accessible = interfaz.isAccessible();
            if (!accessible) interfaz.setAccessible(true);
            ((SourceInterface) interfaz.get(target)).putPacket(target, pk);
            interfaz.setAccessible(accessible);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
            return false;
        }
    }
}

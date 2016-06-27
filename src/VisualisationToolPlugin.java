import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.Nullable;
import org.json.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.prefs.Preferences;


public class VisualisationToolPlugin extends AnAction
{
    private static final String LAST_USED_FOLDER = "last_used_folder";
    private static final int MAX_SESSIONS = 5;
    private List<JsonFile> jsonFilesList;
    private ArrayList<UserSession> sessionsList;
    private Preferences prefs = Preferences.userRoot().node(getClass().getName());
    private Image defaultImage = new ImageIcon(getClass().getResource("/icons/noImage.jpg"))
                                    .getImage()
                                    .getScaledInstance(180, 320, Image.SCALE_SMOOTH);

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        String line = "";
        Runtime runtime = Runtime.getRuntime();
        String SDK_path = System.getenv("ANDROID_HOME");
        if (!SDK_path.isEmpty())
        {
            String ADB_path = SDK_path + "\\platform-tools\\adb.exe";
            if (new File(ADB_path).exists())
            {
                try
                {
                    Path dir = Paths.get(System.getProperty("user.dir"));
                    //need to create new directory HPActionAnalysis?
                    //Path destDir = dir.resolve("HPActionAnalysis");
                    Process process = runtime.exec(new String[]
                            {
                                ADB_path,
                                "pull",
                                "/sdcard/HPActionAnalysis/com.kayak.android",
                                dir.toString()
                            });
                    process.waitFor();
                } catch (Exception e)
                {
                    System.err.format("ADB Exception: %s%n", e);
                }
            }
        }

        Path dir = chooseDir();
        if (dir == null)
            return;

        if (!initJsonFilesList(dir))
            return;
        initSessionsList();
        new ThreadsToolMainWindow("Threads Visualisation Tool", sessionsList);
    }

    private Boolean initJsonFilesList(Path dir)
    {
        this.jsonFilesList = new ArrayList<>();
        String jsonString;

        try (DirectoryStream<Path> stream =
             Files.newDirectoryStream(dir, "*.json")) {
            for (Path filePath: stream)
            {
                if (Files.isReadable(filePath))
                {
                    jsonString = new String(Files.readAllBytes(filePath));
                    JSONObject jsonObject = new JSONObject(jsonString);
                    JsonFile newJsonFile = new JsonFile();
                    initJsonFile(newJsonFile, jsonObject, dir);
                    jsonFilesList.add(newJsonFile);
                }
                else {
                   System.out.println(filePath + "not readable");
                }
            }
        }
        catch (JSONException e)
        {
            JOptionPane.showMessageDialog(new JFrame(),
                    e.getMessage() + "\nThe visualization may be incorrect.",
                    "Error parsing a Json file",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        catch (IOException x){
            System.err.format("IOException: %s%n", x);
            return false;
        }
        return true;
    }

    @Nullable
    private Path chooseDir()
    {
        File defaultFolder = new File(Paths.get(System.getProperty("user.home")).toString());
        JFileChooser chooser = new JFileChooser(prefs.get(LAST_USED_FOLDER, defaultFolder.getAbsolutePath()));
        chooser.setDialogTitle("Choose directory of json files");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setApproveButtonText("OK");

        File choice = null;
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            if (chooser.getSelectedFile() != null)
            {
                if (chooser.getSelectedFile().exists())
                {
                    choice = chooser.getSelectedFile();
                }
                else
                {
                    choice = new File(chooser.getSelectedFile().getParent());
                }
            }
        }
        else
        {
            return null;
        }

        if (choice == null || noJsonFiles(choice))
        {
            JOptionPane.showMessageDialog(null,"No valid directory was chosen.", "Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        else
        {
            prefs.put(LAST_USED_FOLDER, choice.getParent());
            return Paths.get(choice.getAbsolutePath());
        }
    }

    private boolean noJsonFiles(File choice)
    {
        Boolean res = true;
        if (choice.exists() && choice.isDirectory())
        {
            File[] jsonFilesInside = choice.listFiles((dir, name) -> {
                return name.toLowerCase().endsWith(".json");
            });

            if (jsonFilesInside.length > 0)
                res = false;
        }
        return res;
    }

    private void initJsonFile(JsonFile newJsonFile, JSONObject jsonObject, Path dir)
    {
        newJsonFile.model = jsonObject.optString("model");
        newJsonFile.vendor = jsonObject.optString("vendor");
        newJsonFile.OSName = jsonObject.optString("osname");
        newJsonFile.OSVer = jsonObject.optString("osver");
        newJsonFile.carrier = jsonObject.optString("carrier");
        newJsonFile.netType = jsonObject.optString("nettype");
        newJsonFile.infuServer = jsonObject.optString("infuserver");
        newJsonFile.channel = jsonObject.optString("ch");
        newJsonFile.appVerName = jsonObject.optString("appvername");
        newJsonFile.appVerCode = jsonObject.optString("appvercode");
        newJsonFile.sentTime = jsonObject.optLong("senttime");

        if (jsonObject.has("session_start_time"))
            newJsonFile.sessionStartTime = jsonObject.getLong("session_start_time");
        else
            newJsonFile.sessionStartTime = jsonObject.optLong("time");

        newJsonFile.version = jsonObject.optLong("version");
        newJsonFile.deviceId = jsonObject.optString("deviceid");
        newJsonFile.rumAppKey = jsonObject.optString("rumappkey");

        JSONArray arr = jsonObject.optJSONArray("actions");
        if (arr != null)
            for (int i = 0; i < arr.length(); i++)
            {
                JSONObject obj = arr.getJSONObject(i);
                UserAction newAction = new UserAction();

                newAction.name = obj.optString("action_name");
                newAction.id = obj.optLong("actionid");
                newAction.duration = obj.optInt("duration");
                newAction.uaSeq = obj.optInt("ua_seq");
                newAction.startTime = obj.optLong("starttime");
                newAction.ctxName = obj.optString("ctx_name");
                newAction.setThreadsBlob(obj.getString("threadsinfo"));

                newAction.image = defaultImage;
                Path imagePath = dir.resolve(String.valueOf(newAction.id) + ".png");
                Image img;
                try
                {
                    if (Files.exists(imagePath))
                    {
                        img = ImageIO.read(new File(imagePath.toString()));
                        newAction.image = img.getScaledInstance(180, 320, Image.SCALE_SMOOTH);
                    }
                } catch (IOException e)
                {
                    System.err.format("IOException: %s%n", e);
                }

                newJsonFile.actions.add(newAction);
            }
    }

    private void initSessionsList()
    {
        this.sessionsList = new ArrayList<>();
        if (jsonFilesList.isEmpty())
            return;

        Collections.sort(jsonFilesList);
        JsonFile []allJsonFiles = new JsonFile[jsonFilesList.size()];
        jsonFilesList.toArray(allJsonFiles);

        for (int i = 0; i < allJsonFiles.length; i++)
        {
            if (i != 0 && isSameSession(allJsonFiles[i], allJsonFiles[i-1]))
            {
                UserSession lastAddedSession = sessionsList.get(sessionsList.size()-1);
                lastAddedSession.mergeWith(allJsonFiles[i].actions);
            }
            else
            {
                sessionsList.add(new UserSession(allJsonFiles[i]));
                if (sessionsList.size() >= MAX_SESSIONS)
                    break;
            }
        }
    }

    private boolean isSameSession(JsonFile file1, JsonFile file2)
    {
        return (file1.compareTo(file2) == 0);
    }
}


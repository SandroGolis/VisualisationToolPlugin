import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.json.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Created by Sandro on 4/30/2016.
 */
public class VisualisationToolPlugin extends AnAction
{
    private List<ThreadInfo> threadsList;
    private List<List<ThreadInfo> > sessions;

    public VisualisationToolPlugin()
    {
        initThreadsList();
        initSessions();
    }

    private void initThreadsList()
    {
        this.threadsList = new ArrayList<>();
        String jsonString;
        //TODO: dynamically find a relevant path. Alert if no jsons found
        Path dir = Paths.get(System.getProperty("user.home"),
                "IdeaProjects",
                "VisualisationToolPlugin",
                "temp",
                "com.kayak.android");

        try (DirectoryStream<Path> stream =
             Files.newDirectoryStream(dir, "*.json")) {
            for (Path filePath: stream)
            {
                if (Files.isReadable(filePath))
                {
                    jsonString = new String(Files.readAllBytes(filePath));
                    JSONObject jsonObject = new JSONObject(jsonString);
                    ThreadInfo newThread = new ThreadInfo();
                    initThread(newThread, jsonObject, dir);
                    threadsList.add(newThread);
                }
                else {
                   System.out.println(filePath + "not readable");
                }
            }
        }
        catch (IOException x){
            System.err.format("IOException: %s%n", x);
        }

        // sort threadsList from newest to oldest actions
        Collections.sort(threadsList);
    }

    private void initThread(ThreadInfo newThread, JSONObject jsonObject, Path dir)
    {
        newThread.model = jsonObject.getString("model");
        newThread.osName = jsonObject.getString("osname");
        newThread.sessionStartTime = jsonObject.getLong("session_start_time");

        JSONArray arr = jsonObject.getJSONArray("actions");
        for (int i=0; i<arr.length(); i++)
        {
            JSONObject obj = arr.getJSONObject(i);
            ThreadAction newAction = new ThreadAction();

            newAction.name = obj.getString("action_name");
            newAction.id = obj.getLong("actionid");
            newAction.duration = obj.getInt("duration");
            newAction.uaSeq = obj.getInt("ua_seq");
            newAction.startTime = obj.getLong("starttime");
            newAction.ctxName = obj.getString("ctx_name");
            newAction.info = obj.getString("threadsinfo");

            Path imagePath = dir.resolve(String.valueOf(newAction.id) + ".png");
            if (Files.exists(imagePath))
                newAction.imagePath = imagePath;
            else
                newAction.imagePath = null;

            newThread.actions.add(newAction);
        }
    }

    private void initSessions()
    {
        this.sessions = new ArrayList<>();
        int i = 0;
        while ( i < this.size()) {
            long currentSession = this.threadsList.get(i).getSessionStartTime();
            List<ThreadInfo> session = new ArrayList<>();
            while ( i < this.size() && this.threadsList.get(i).getSessionStartTime() == currentSession) {
                session.add(this.threadsList.get(i));
                i++;
            }
            List<ThreadInfo> se = new ArrayList<>(session);
            this.sessions.add((se));
            i++;
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        new ThreadsToolMainWindow("Threads Visualisation Tool", sessions);
    }

    public int size() {
        return threadsList.size();
    }

    public int numOfSessions()
    {
        return sessions.size();
    }



}


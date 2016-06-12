import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

enum ThreadActionType {
    METHOD,
    NETWORK,
    BLOCKING
}

public class Thread implements Comparable<Thread>
{
    private int threadId;
    int earliestStart = Integer.MAX_VALUE;
    int latestEnd = Integer.MIN_VALUE;
    List<ThreadAction> threadActions = new ArrayList<>();
    public Thread(int threadId)
    {
        this.threadId = threadId;
    }

    void addAction(ThreadAction newThreadAction)
    {
        threadActions.add(newThreadAction);
        if (newThreadAction.startTime < earliestStart)
            earliestStart = newThreadAction.startTime;
        if (newThreadAction.startTime + newThreadAction.duration > latestEnd)
            latestEnd = newThreadAction.startTime + newThreadAction.duration;
    }

    @Override
    public int compareTo(@NotNull Thread o)
    {
        return (threadId < o.threadId) ? -1 : (threadId > o.threadId) ? 1 : 0;
    }

    @Override
    public String toString()
    {
        return " #" + threadId + " (" + threadActions.size() + " tasks)";
    }
}

class ThreadAction
{
    private ThreadActionType type;
    private long userActionId;
    private int threadId;
    private String name;
    int startTime;
    int duration;
    String callingMethod;

    // Relevant for NET threadActionType only
    private String Url;
    private int responseStart;
    private int responseCode;

    ThreadAction(
            ThreadActionType type,
            long userActionId,
            int threadId,
            int startTime,
            int duration,
            String name)
    {
        this.type = type;
        this.userActionId = userActionId;
        this.threadId = threadId;
        this.startTime = startTime;
        this.duration = duration;
        this.name = name;
    }

    ThreadAction(
            ThreadActionType type,
            long userActionId,
            int threadId,
            String name,
            int startTime,
            int duration,
            String url,
            int responseStart,
            int responseCode)
    {
        this.type = type;
        this.userActionId = userActionId;
        this.threadId = threadId;
        this.name = name;
        this.startTime = startTime;
        this.duration = duration;
        Url = url;
        this.responseStart = responseStart;
        this.responseCode = responseCode;
    }
}



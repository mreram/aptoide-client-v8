package cm.aptoide.pt.analytics.analytics;

import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.logger.Logger;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.subscriptions.CompositeSubscription;

public class AptoideBiAnalytics {

  private static final String TAG = AptoideBiAnalytics.class.getSimpleName();
  private final EventsPersistence persistence;
  private final AptoideBiEventService service;
  private final CompositeSubscription subscriptions;
  private final CrashReport crashReport;
  private final int threshHoldSize;
  private final long sendInterval;
  private final Scheduler timerScheduler;

  /**
   * @param threshHoldSize max number of events to batch before send
   * @param sendInterval max time(in milliseconds) interval between event sends
   */
  public AptoideBiAnalytics(EventsPersistence persistence, AptoideBiEventService service,
      CompositeSubscription subscriptions, CrashReport crashReport, Scheduler timerScheduler,
      int threshHoldSize, long sendInterval) {
    this.persistence = persistence;
    this.service = service;
    this.subscriptions = subscriptions;
    this.crashReport = crashReport;
    this.timerScheduler = timerScheduler;
    this.threshHoldSize = threshHoldSize;
    this.sendInterval = sendInterval;
  }

  public void log(String eventName, Map<String, Object> data, AnalyticsManager.Action action,
      String context) {
    persistence.save(new Event(eventName, data, action, context, Calendar.getInstance()
        .getTimeInMillis()))
        .subscribe(() -> {
        }, throwable -> Logger.w(TAG, "cannot save the event due to " + throwable.getMessage()));
  }

  public void setup() {
    subscriptions.add(persistence.getAll()
        .filter(this::shouldSendEvents)
        .flatMapCompletable(events -> persistence.remove(events)
            .andThen(service.send(events))
            .onErrorResumeNext(throwable -> {
              crashReport.log(throwable);
              return persistence.save(events);
            }))
        .retry()
        .subscribe());

    subscriptions.add(Observable.interval(sendInterval, TimeUnit.MILLISECONDS, timerScheduler)
        .flatMap(time -> persistence.getAll()
            .first())
        .filter(events -> events.size() > 0)

        .flatMapCompletable(events -> persistence.remove(events)
            .andThen(service.send(events))
            .onErrorResumeNext(throwable -> persistence.save(events)))
        .doOnError(throwable -> crashReport.log(throwable))
        .retry()
        .subscribe());
  }

  private boolean shouldSendEvents(List<Event> events) {
    return events.size() > 0 && events.size() >= threshHoldSize;
  }
}

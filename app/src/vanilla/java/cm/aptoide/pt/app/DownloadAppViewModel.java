package cm.aptoide.pt.app;

import cm.aptoide.pt.dataprovider.model.v7.GetAppMeta;

/**
 * Created by filipegoncalves on 5/9/18.
 */

public class DownloadAppViewModel {

  private final Action action;
  private final int progress;
  private final DownloadState downloadState;
  private final GetAppMeta.Pay pay;

  public DownloadAppViewModel(Action action, int progress, DownloadState downloadState,
      GetAppMeta.Pay pay) {
    this.action = action;
    this.progress = progress;
    this.downloadState = downloadState;
    this.pay = pay;
  }

  public Action getAction() {
    return action;
  }

  public int getProgress() {
    return progress;
  }

  public DownloadState getDownloadState() {
    return downloadState;
  }

  public boolean isDownloading() {
    return downloadState.equals(DownloadState.ACTIVE)
        || downloadState.equals(DownloadState.PAUSE)
        || downloadState.equals(DownloadState.INDETERMINATE);
  }

  public GetAppMeta.Pay getPay() {
    return pay;
  }

  public boolean hasError() {
    return downloadState.equals(DownloadState.ERROR) || downloadState.equals(
        DownloadState.NOT_ENOUGH_STORAGE_ERROR);
  }

  public enum Error {
    NETWORK, GENERIC
  }

  public enum Action {
    UPDATE, INSTALL, DOWNGRADE, OPEN, PAY
  }

  public enum DownloadState {
    ACTIVE, PAUSE, COMPLETE, INDETERMINATE, ERROR, NOT_ENOUGH_STORAGE_ERROR
  }
}

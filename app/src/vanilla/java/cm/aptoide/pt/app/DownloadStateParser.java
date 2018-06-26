package cm.aptoide.pt.app;

import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.download.InstallType;
import cm.aptoide.pt.download.Origin;
import cm.aptoide.pt.install.Install;

/**
 * Created by filipegoncalves on 5/9/18.
 */

public class DownloadStateParser {

  public DownloadStateParser() {
  }

  public DownloadAppViewModel.DownloadState parseDownloadState(Install.InstallationStatus state) {
    DownloadAppViewModel.DownloadState downloadState;
    switch (state) {
      case INSTALLING:
        downloadState = DownloadAppViewModel.DownloadState.ACTIVE;
        break;
      case PAUSED:
        downloadState = DownloadAppViewModel.DownloadState.PAUSE;
        break;
      case IN_QUEUE:
        downloadState = DownloadAppViewModel.DownloadState.INDETERMINATE;
        break;
      case INSTALLED:
        downloadState = DownloadAppViewModel.DownloadState.COMPLETE;
        break;
      case UNINSTALLED:
        downloadState = DownloadAppViewModel.DownloadState.COMPLETE;
        break;
      case INSTALLATION_TIMEOUT:
      case GENERIC_ERROR:
        downloadState = DownloadAppViewModel.DownloadState.ERROR;
        break;
      case NOT_ENOUGH_SPACE_ERROR:
        downloadState = DownloadAppViewModel.DownloadState.NOT_ENOUGH_STORAGE_ERROR;
        break;
      default:
        downloadState = DownloadAppViewModel.DownloadState.COMPLETE;
        break;
    }
    return downloadState;
  }

  public DownloadAppViewModel.Action parseDownloadType(Install.InstallationType type,
      boolean paidApp, boolean wasPaid) {
    DownloadAppViewModel.Action action;
    if (paidApp && !wasPaid) {
      action = DownloadAppViewModel.Action.PAY;
    } else {
      switch (type) {
        case INSTALLED:
          action = DownloadAppViewModel.Action.OPEN;
          break;
        case INSTALL:
          action = DownloadAppViewModel.Action.INSTALL;
          break;
        case DOWNGRADE:
          action = DownloadAppViewModel.Action.DOWNGRADE;
          break;
        case UPDATE:
          action = DownloadAppViewModel.Action.UPDATE;
          break;
        default:
          action = DownloadAppViewModel.Action.INSTALL;
          break;
      }
    }
    return action;
  }

  public int parseDownloadAction(DownloadAppViewModel.Action action) {
    int downloadAction;
    switch (action) {
      case INSTALL:
        downloadAction = Download.ACTION_INSTALL;
        break;
      case UPDATE:
        downloadAction = Download.ACTION_UPDATE;
        break;
      case DOWNGRADE:
        downloadAction = Download.ACTION_DOWNGRADE;
        break;
      default:
        throw new IllegalArgumentException("Invalid action");
    }
    return downloadAction;
  }

  public Origin getOrigin(int action) {
    switch (action) {
      default:
      case Download.ACTION_INSTALL:
        return Origin.INSTALL;
      case Download.ACTION_UPDATE:
        return Origin.UPDATE;
      case Download.ACTION_DOWNGRADE:
        return Origin.DOWNGRADE;
    }
  }

  public InstallType getInstallType(int action) {
    switch (action) {
      default:
      case Download.ACTION_INSTALL:
        return InstallType.INSTALL;
      case Download.ACTION_UPDATE:
        return InstallType.UPDATE;
      case Download.ACTION_DOWNGRADE:
        return InstallType.DOWNGRADE;
    }
  }
}

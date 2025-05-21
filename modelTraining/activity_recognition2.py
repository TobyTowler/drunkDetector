import os
import pandas as pd
import numpy as np
import statsmodels.api as sm
import matplotlib.pyplot as plt
from collections import Counter
import copy
from sklearn.metrics import confusion_matrix

from shap.explainers.other import Random
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import cross_val_score, train_test_split, StratifiedKFold, GridSearchCV
from sklearn.ensemble import AdaBoostClassifier, RandomForestClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.feature_selection import RFE
import itertools
from scipy.stats import skew, kurtosis
from scipy import stats
from sklearn import metrics
import shap
from statsmodels.compat import scipy

from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import joblib

# Class
activity_indices = {
  'Stationary': 0,
  'Walking-flat-surface': "Not Drunk", # Not Drunk
  'Walking-up-stairs': 2,
  'Walking-down-stairs': 3,
  'Elevator-up': 4,
  'Running': "Drunk", # Drunk
  'Elevator-down': 6
}


def compute_raw_data(dir_name):

  raw_data_features = None
  raw_data_labels = None
  interpolated_timestamps = None

  sessions = set()
  # Categorize files containing different sensor sensor data
  file_dict = dict()
  # List of different activity names
  activities = set()
  file_names = os.listdir(dir_name)

  for file_name in file_names:
    if "labels" in file_name:
      continue
    if "pressure" in file_name:
      continue
    if '.txt' in file_name:
      tokens = file_name.split('-')
      identifier = '-'.join(tokens[4: 6])
      activity = '-'.join(file_name.split('-')[6:-2])

      sensor = tokens[-1]
      sessions.add((identifier, activity))

      if (identifier, activity, sensor) in file_dict:
        file_dict[(identifier, activity, sensor)].append(file_name)
      else:
        file_dict[(identifier, activity, sensor)] = [file_name]


  for session in sessions:
    accel_file = file_dict[(session[0], session[1], 'accel.txt')][0]
    accel_df = pd.read_csv(dir_name + '/' + accel_file)
    accel = accel_df.drop_duplicates(accel_df.columns[0], keep='first').values
    if accel.shape[0] == 0:
      print(f"Warning: No accel data for session {session}")
      continue
    # Spine-line interpolataion for x, y, z values (sampling rate is 32Hz).
    # Remove data in the first and last 3 seconds.
    timestamps = np.arange(accel[0, 0]+3000.0, accel[-1, 0]-3000.0, 1000.0/32)

    accel = np.stack([np.interp(timestamps, accel[:, 0], accel[:, 1]),
                      np.interp(timestamps, accel[:, 0], accel[:, 2]),
                      np.interp(timestamps, accel[:, 0], accel[:, 3])],
                     axis=1)

    gyro_file = file_dict[(session[0], session[1], 'gyro.txt')][0]
    gyro_df = pd.read_csv(dir_name + '/' + gyro_file)
    gyro = gyro_df.drop_duplicates(gyro_df.columns[0], keep='first').values
    if gyro.shape[0] == 0:
      print(f"Warning: No gyro data for session {session}")
      continue
    gyro = np.stack([np.interp(timestamps, gyro[:, 0], gyro[:, 1]),
                      np.interp(timestamps, gyro[:, 0], gyro[:, 2]),
                      np.interp(timestamps, gyro[:, 0], gyro[:, 3])],
                     axis=1)

    # Keep data with dimension multiple of 128
    length_multiple_128 = 128*int(accel.shape[0]/128)
    accel = accel[0:length_multiple_128, :]
    gyro = gyro[0:length_multiple_128, :]
    labels = np.array(accel.shape[0]*[activity_indices[session[1]]]).reshape(-1, 1)
    timestamps = timestamps[0:length_multiple_128]

    if raw_data_features is None:
      raw_data_features = np.append(accel, gyro, axis=1)
      raw_data_labels = labels
      interpolated_timestamps = timestamps
    else:
      raw_data_features = np.append(raw_data_features, np.append(accel, gyro, axis=1), axis=0)
      raw_data_labels = np.append(raw_data_labels, labels, axis=0)
      interpolated_timestamps = np.append(interpolated_timestamps, timestamps, axis=0)
  return raw_data_features, raw_data_labels, interpolated_timestamps

# Remember, the output of this is 3 arrays, all of length n
# features entry 0 : accelx, accely, accelz, bar, gyro, gyro, gyro. data labels entry 0 : i.e. running. timestamps entry 0 : 103030435.




# We don't need this
"""
def plot_raw_data(raw_data_features, raw_data_labels):
   This function plots the raw data features (after applying basic data processing) and raw data labels.
      The first subplot is the the accelerometer magnitude. The second subplot is the barometric pressure.
      The third subplot is the activity label (check "activity_indices" to see what activity each index corresponds to).
  
  accel_magnitudes = np.sqrt((raw_data_features[:, 0]**2).reshape(-1, 1)+
                             (raw_data_features[:, 1]**2).reshape(-1, 1)+
                             (raw_data_features[:, 2]**2).reshape(-1, 1))

  plt.subplot(3, 1, 1)
  plt.plot(accel_magnitudes)
  plt.xticks(fontsize=8)
  plt.ylabel('Acceleration (m/s^2)', fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Accelerometer Magnitude', fontsize=8)

  plt.subplot(3, 1, 2)
  plt.plot(raw_data_features[:, 3])
  plt.xticks(fontsize=8)
  plt.ylabel('Pressure (mbar)', fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Barometric Pressure', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(raw_data_labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('Activity', fontsize=8)
  plt.gca().set_title('Activity Label', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 1 Raw Data.png")
  plt.show()
"""

def feature_extraction_from_segment(segment):

  segment = segment.flatten()

  mean = np.mean(segment)
  var = np.var(segment)
  max = np.max(segment)
  min = np.min(segment)
  kurtosis = stats.kurtosis(segment)
  skew = stats.skew(segment)
  energy = np.sum(segment ** 2)
  zero_crossings = np.count_nonzero(np.diff(np.sign(segment)))

  freq = np.abs(np.fft.fft(segment))[:len(segment) // 2]  # fft output should be symmetrical, so cut in half
  peak = np.max(freq)
  power = np.sum(freq ** 2)
  mean_amplitude = np.mean(freq)

  dom_freq_index = np.argmax(freq)
  dom_freq = dom_freq_index * (32 / len(freq))  # sampling rate is 32 Hz

  freq_normalised = freq / np.sum(freq)
  entropy = -np.sum(freq_normalised * np.log2(freq_normalised + 1e-12))  # Add small value to avoid log(0)

  jerk = np.diff(segment) * 32  # sampling frequency is every 1/32 seconds
  mean_jerk = np.mean(np.abs(jerk))

  return np.array([mean, var, max, min, kurtosis, skew, energy, zero_crossings, peak, power, mean_amplitude, dom_freq, entropy, mean_jerk])

def feature_extraction(raw_data_features, raw_data_labels, timestamps):
  """    raw_data_features: The fourth column is the barometer data.

  Returns:
    features: Features extracted from the data features, where
              features[:, 0] is the mean magnitude of acceleration;
              features[:, 1] is the variance of acceleration;
              features[:, 2:6] is the fft power spectrum of equally-spaced frequencies;
              features[: 6:12] is the fft power spectrum of frequencies in logarithmic sacle;
              features[:, 13] is the slope of pressure.
  Args:

  """
  features = None
  labels = None
  accel_magnitudes = np.sqrt((raw_data_features[:, 0] ** 2).reshape(-1, 1)+
                             (raw_data_features[:, 1] ** 2).reshape(-1, 1)+
                             (raw_data_features[:, 2] ** 2).reshape(-1, 1))
  gyro_magnitudes = np.sqrt((raw_data_features[:, 3] ** 2).reshape(-1, 1) +
                             (raw_data_features[:, 4] ** 2).reshape(-1, 1) +
                             (raw_data_features[:, 5] ** 2).reshape(-1, 1))

  # The window size for feature extraction
  segment_size = 128


  for i in range(0, accel_magnitudes.shape[0] - segment_size, 64):

    feature = np.array([])

    accel_segment = accel_magnitudes[i:i + segment_size, :]

    feature = np.append(feature, feature_extraction_from_segment(accel_segment))

    accel_ax = raw_data_features[i:i + segment_size, 0]

    feature = np.append(feature, feature_extraction_from_segment(accel_ax))

    accel_ay = raw_data_features[i:i + segment_size, 1]

    feature = np.append(feature, feature_extraction_from_segment(accel_ay))

    accel_az = raw_data_features[i:i + segment_size, 2]

    feature = np.append(feature, feature_extraction_from_segment(accel_az))

    accel_corrs = np.array([
      np.corrcoef(accel_ax, accel_ay)[0, 1],  # the return type is a matrix something like [ [ 1, corr ] , [ corr, 1] ]
      np.corrcoef(accel_ay, accel_az)[0, 1], # the correlation between ax and ax or az and az is 1 (obviously)
      np.corrcoef(accel_az, accel_ax)[0, 1]  # and the correlation between ax and az is the same as az and ax
    ])

    feature = np.append(feature, accel_corrs)

    gyro_segment = gyro_magnitudes[i:i+segment_size, :]

    feature = np.append(feature, feature_extraction_from_segment(gyro_segment))

    gyro_rx = raw_data_features[i:i + segment_size, 3]

    feature = np.append(feature, feature_extraction_from_segment(gyro_rx))

    gyro_ry = raw_data_features[i:i + segment_size, 4]

    feature = np.append(feature, feature_extraction_from_segment(gyro_ry))

    gyro_rz = raw_data_features[i:i + segment_size, 5]

    feature = np.append(feature, feature_extraction_from_segment(gyro_rz))

    gyro_corrs = np.array([
      np.corrcoef(gyro_rx, gyro_ry)[0, 1],  # the return type is a matrix something like [ [ 1, corr ] , [ corr, 1] ]
      np.corrcoef(gyro_ry, gyro_rz)[0, 1],  # the correlation between ax and ax or az and az is 1 (obviously)
      np.corrcoef(gyro_rz, gyro_rx)[0, 1]  # and the correlation between ax and az is the same as az and ax
    ])

    feature = np.append(feature, gyro_corrs)

    if features is None:
      features = np.array([feature])
    else:
      features = np.append(features, [feature], axis=0)

    label = Counter(raw_data_labels[i:i+segment_size][:, 0].tolist()).most_common(1)[0][0]

    if labels is None:
      labels = np.array([label])
    else:
      labels = np.append(labels, [label], axis=0)

  return features, labels

"""
def plot_extracted_features(features, labels):
   This function plots the extracted features. The top plot is the variance of accelerometer magnitude data.
      The middle plot is the slope of barometric pressure data. The bottom plot is the activity label.
  
  # Plot the acceleration variance

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 0])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('Mean of Accelerometer Magnitude', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 1])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('Variance of Accelerometer Magnitude', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 3 Feature Data.png")
  plt.show()
  plt.close()

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 2])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum equal space band 1', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 3])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum equal space band 2', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 3 Feature Data.png")
  plt.show()
  plt.close()

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 4])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum equal space band 3', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 5])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum equal space band 4', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 4 Feature Data.png")
  plt.show()
  plt.close()

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 6])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 1', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 7])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 2', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 5 Feature Data.png")
  plt.show()
  plt.close()

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 8])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 3', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 9])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 4', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 6 Feature Data.png")
  plt.show()
  plt.close()

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 10])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 5', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 11])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 6', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 7 Feature Data.png")
  plt.show()
  plt.close()

  plt.subplot(3, 1, 1)
  plt.plot(features[:, 12])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('m^2/s^4', fontsize=8)
  plt.gca().set_title('fft Power Spectrum logarithmic band 7', fontsize=8)

  # Plot the barometer slope
  plt.subplot(3, 1, 2)
  plt.plot(features[:, 13])
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.ylabel('mbars/s', fontsize=8)
  plt.gca().set_title('Slope of Pressure', fontsize=8)

  plt.subplot(3, 1, 3)
  plt.plot(labels)
  plt.xticks(fontsize=8)
  plt.yticks(fontsize=8)
  plt.gca().set_title('Activity', fontsize=8)
  plt.grid(True)
  plt.savefig("Fig. 8 Feature Data.png")
  plt.show()
"""

def five_fold_cross_validation(features, labels):
  true_labels = list()
  predicted_labels = list()

  for train_index, test_index in StratifiedKFold(n_splits=5).split(features, labels):
    X_train = features[train_index, :]
    Y_train = labels[train_index]

    X_test = features[test_index, :]
    Y_test = labels[test_index]

    clf = DecisionTreeClassifier()
    clf.fit(X_train, Y_train)
    predicted_label = clf.predict(X_test)

    predicted_labels += predicted_label.flatten().tolist()
    true_labels += Y_test.flatten().tolist()

  # Given N different activities, the confusion matrix is a N*N matrix
  confusion_matrix = np.zeros((len(activity_indices), len(activity_indices)))

  for i in range(len(true_labels)):
    confusion_matrix[int(true_labels[i]), int(predicted_labels[i])] += 1

  # Normalized confusion matrix
  #for i in range(confusion_matrix.shape[0]):
   # print(sum(confusion_matrix[i, :]))
   #  confusion_matrix[i, :] = confusion_matrix[i, :]/sum(confusion_matrix[i, :])

  print("===== Confusion Matrix (within subject) =====")
  plot_confusion_matrix(confusion_matrix, activity_indices.keys(), normalize=False)
  plt.show()


def evaluate_generalized_model(X_train, Y_train, X_test, Y_test):
  clf = DecisionTreeClassifier().fit(X_train, Y_train)
  Y_pred = clf.predict(X_test)

  # # Plot the true labels and predicted labels
  # plt.subplot(2, 1, 1)
  # plt.plot(Y_test)
  #
  # plt.subplot(2, 1, 2)
  # plt.plot(Y_pred)
  #
  # plt.show()

  # Given N activities, the confusion matrix is a N*N matrix
  confusion_matrix = np.zeros((len(activity_indices), len(activity_indices)))

  for i in range(len(Y_test)):
    confusion_matrix[int(Y_test[i]), int(Y_pred[i])] += 1

  # print(confusion_matrix)

  # for i in range(confusion_matrix.shape[0]):
  #   # print(sum(confusion_matrix[i, :]))
  #   confusion_matrix[i, :] = confusion_matrix[i, :]/sum(confusion_matrix[i, :])

  print("===== Confusion Matrix (between subjects) =====")
  # plt.imshow(confusion_matrix)
  # plt.show()
  plot_confusion_matrix(confusion_matrix, activity_indices.keys())
  plt.show()

  # Print the top-5 features
  selector = RFE(estimator=clf, n_features_to_select=5)
  selector.fit(X_train, Y_train)
  print("===== Mask of Top-5 Features =====")
  print(selector.support_)

  explainer = shap.TreeExplainer(clf)
  shap_values = explainer.shap_values(X_test)
  print(f"Type of X_test: {type(X_test)}")
  print(f"Shape of X_test: {X_test.shape}")
  print(f"Type of shap_values[{i}]: {type(shap_values[i])}")
  print(f"Shape of shap_values[{i}]: {shap_values[i].shape}")

  num_classes = shap_values.shape[2]

  for i in range(num_classes):
    shap.summary_plot(shap_values[:, :, i], X_test)


def plot_confusion_matrix(confusion_matrix, classes, normalize=False, title='Confusion Matrix', cmap=plt.cm.Blues):
    """ This function prints and plots the confusion matrix.
        Normalization can be applied by setting `normalize=True`.
    """
    if normalize:
        confusion_matrix = confusion_matrix.astype('float') / confusion_matrix.sum(axis=1)[:, np.newaxis]
        print("Normalized confusion matrix")
    else:
        print('Confusion matrix, without normalization')

    print(confusion_matrix)

    plt.imshow(confusion_matrix, interpolation='nearest', cmap=cmap)
    plt.title(title)
    plt.colorbar()
    tick_marks = np.arange(len(classes))
    plt.xticks(tick_marks, classes, rotation=45)
    plt.yticks(tick_marks, classes)

    fmt = '.2f' if normalize else '.0f'
    thresh = confusion_matrix.max() / 2.
    for i, j in itertools.product(range(confusion_matrix.shape[0]), range(confusion_matrix.shape[1])):
        plt.text(j, i, format(confusion_matrix[i, j], fmt),
                 horizontalalignment="center",
                 color="white" if confusion_matrix[i, j] > thresh else "black")

    plt.tight_layout()
    plt.ylabel('True label')
    plt.xlabel('Predicted label')
    plt.show()


if __name__ == "__main__":

  data_path = '../testData/'
  train_id = 'uploadedGeorge'

  #raw_data_features, raw_data_labels, timestamps = compute_raw_data(data_path + train_id)


  "***Plot Raw Data***"
  # Plot the raw data to get a sense about what features might work.
  # You can comment out this line of code if you don't want to see the plots
  #plot_raw_data(raw_data_features, raw_data_labels)


  "***Feature Extraction***"
  #features, labels = feature_extraction(raw_data_features, raw_data_labels, timestamps)


  "***Plot Features***"
  # You can comment out this line of code if you don't want to see the plots
  #plot_extracted_features(features, labels)


  "***Classify User's Own Data***"
  #five_fold_cross_validation(features, labels)


  "***Person-independent model (i.e. train on other's data and test on your own data)***"
  data = None
  dataLabels = None
  X_train = None
  Y_train = None
  X_test = None
  Y_test = None

  dirs = os.listdir(data_path)

  for dir in dirs:
    print("Processing data from %s" %dir)
    raw_data_features, raw_data_labels, timestamps = compute_raw_data(data_path + dir)
    features, labels = feature_extraction(raw_data_features, raw_data_labels, timestamps)

    if data is None:
      data = features
      dataLabels = labels
    else:
      data = np.append(data, features, axis=0)
      dataLabels = np.append(dataLabels, labels, axis=0)

  X_train, X_test, Y_train, Y_test = train_test_split(data, dataLabels, test_size=0.2, random_state=0)

  clf = RandomForestClassifier(n_estimators=100, random_state=0)
  skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=0)

  if data is None or data.shape[0] == 0:
    raise ValueError("No training data was extracted. Check input directories or feature extraction.")
  clf.fit(X_train, Y_train)
  selector = RFE(estimator=clf, n_features_to_select=20)
  selector.fit(X_train, Y_train)

  print("===== Feature importances =====")
  importances = clf.feature_importances_
  sorted_idx = np.argsort(importances)[::-1]
  print (sorted_idx)

  selected_indices = np.where(selector.support_)[0]
  print("Top-20 selected feature indices:", selected_indices)

  selected_indices = selector.get_support(indices=True)

  X_train_reduced = selector.transform(X_train)
  X_test_reduced = selector.transform(X_test)

  clf_reduced = RandomForestClassifier(n_estimators=100, random_state=0)
  #clf_reduced.fit(X_train_reduced, Y_train)

  param_grid = {
    'n_estimators': [100, 300, 500],
    'max_depth': [None, 10, 20],
    'min_samples_split': [2, 5],
    'min_samples_leaf': [1, 2],
    'max_features': ['sqrt', 'log2'],
    'class_weight': ['balanced']  # Useful if you have class imbalance
  }

  grid_search_rf = GridSearchCV(
    clf_reduced,
    param_grid,
    cv=5,
    scoring='accuracy',
    n_jobs=-1,
    verbose=2,
    return_train_score=True
  )
  grid_search_rf.fit(X_train_reduced, Y_train)
  model = grid_search_rf.best_estimator_
  print("Best parameters found:", grid_search_rf.best_params_)
  print("Best cross-val score:", grid_search_rf.best_score_)
  #model.fit(X_train_reduced, Y_train)

  Y_predict = model.predict(X_test_reduced)

  # Basic accuracy
  print("Accuracy:", accuracy_score(Y_test, Y_predict))

  # Detailed metrics
  print(classification_report(Y_test, Y_predict))

  cm = confusion_matrix(Y_test, Y_predict)

  plot_confusion_matrix(cm, ["Drunk", "Not Drunk"], title="Confusion Matrix of Drunk Prediction")

  # Confusion matrix
  print(cm)

  explainer = shap.TreeExplainer(model)
  shap_values = explainer.shap_values(X_test_reduced)
  num_classes = shap_values.shape[2]

  for i in range(num_classes):
    shap.summary_plot(shap_values[:, :, i], X_test_reduced)

  #scores = cross_val_score(clf_reduced, X_train_reduced, Y_train, cv=skf, scoring='accuracy')
  #print(f"Mean cross score: {np.mean(scores)}")

  initial_type = [('float_input', FloatTensorType([None, X_train_reduced.shape[1]]))]
  onnx_model = convert_sklearn(model, initial_types=initial_type)
  with open("drunk_model.onnx", "wb") as f:
    f.write(onnx_model.SerializeToString())

  #evaluate_generalized_model(X_train, Y_train, X_test, Y_test)


#Best parameters found: {'class_weight': 'balanced', 'max_depth': None, 'max_features': 'sqrt', 'min_samples_leaf': 1, 'min_samples_split': 2, 'n_estimators': 100}
# Best cross-val score: 0.9623737770104117
# Accuracy: 0.9595375722543352
#               precision    recall  f1-score   support
#
#        Drunk       0.90      0.92      0.91        75
#    Not Drunk       0.98      0.97      0.97       271
#
#     accuracy                           0.96       346
#    macro avg       0.94      0.95      0.94       346
# weighted avg       0.96      0.96      0.96       346
#
# Confusion matrix, without normalization
# [[ 69   6]
#  [  8 263]]
# [[ 69   6]
#  [  8 263]]

# ===== Feature importances =====
# [  5  32  41   4  13   1 114  42  27  49 100  47  15   2 117  50  77  34
#   36  37  25  62  31  81  10 110  28  85  33  55  69  76  59 102  86  93
#   58  82  74  12  24 103  38  22 109  96   3  79  51  68  72  83 115 105
#   43  88  14  67  18 111  65  90  56  78  17  26  60 106  97  84  23   9
#   46  21  75  57  73  71  54  48  89  20  64   0 108 107 116  29  99   8
#  104   6  40  16  61  45 101 113  44  92  87  52  19  94  95  30  91  53
#   63 112  80  98  35  11  39   7  66  70]
# Top-20 selected feature indices: [  2   4   5  12  13  32  36  37  41  42  43  47  49  50  55  77  79  81
#  100 117]




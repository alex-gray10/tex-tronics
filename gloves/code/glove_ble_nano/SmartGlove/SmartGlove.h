#ifndef _H__SMARTGLOVE_
#define _H__SMARTGLOVE_

#define BLE_DEVICE_NAME       "SmartGlove"  // Local Device Name
#define DATA_REFRESH_RATE_MS  500           // Delay between data collection (milliseconds)

#define UUID_IMU_SERVICE      0x4000        // This Service contains IMU data (accel, gyro, and magnetometer)
#define UUID_ACCEL_CHAR       0x4001        // Holds the accelerometer data (x-axis, y-axis, and z-axis)
#define UUID_GYRO_CHAR        0x4002        // Holds the gyroscope data (x-axis, y-axis, and z-axis)
#define UUID_MAG_CHAR         0x4003        // Holds the magnetometer data (x-axis, y-axis, and z-axis)
#define UUID_FLEX_SERVICE     0x4004        // This Service contains FlexSensor data (all 5 fingers)
#define UUID_THUMB_CHAR       0x4005        // Thumb angle
#define UUID_INDEX_CHAR       0x4006        // Index Finger angle
#define UUID_MIDDLE_CHAR      0x4007        // Middle Finger angle
#define UUID_RING_CHAR        0x4008        // Ring Finger angle
#define UUID_PINKY_CHAR       0x4009        // Pinky Finger angle
#define UUID_SMART_SERVICE    0x400A        // This Service holds misc. data related to the SmartGlove
#define UUID_DATA_READY_CHAR  0x400B        // This is used as an indicator when all of the data has been updated and is ready to be read

typedef union imu_data {
  float f;
  uint8_t b[4];  // (Float is 4 Bytes)
} imu_data_t;                               // Use this for Float data that is to be sent via BLE

#define IMU_CHECK_KEY         0x49D4        // Used to ensure 9DoF is connected properly

#endif

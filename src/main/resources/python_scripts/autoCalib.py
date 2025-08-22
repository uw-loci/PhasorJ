#@ ImageJ ij
#@ Img raw_phasor
#@ Img calib_img
#@ double frequency
#@ double lifetime
#@output Img output

import phasorpy
from phasorpy.phasor import phasor_calibrate
from phasorpy.phasor import phasor_from_signal
import numpy as np

raw_phasor_arr = ij.py.to_xarray(raw_phasor)
raw_phasor_arr = raw_phasor_arr.values.transpose(1, 2, 0).copy()
real = raw_phasor_arr[:, :, 1]
imag = raw_phasor_arr[:, :, 2]

calib_arr = ij.py.to_xarray(calib_img)
calib_arr = calib_arr.values.transpose(1, 2, 0).copy()
calib_phasor = phasor_from_signal(calib_arr)
reference_mean = calib_phasor[0]
reference_real = calib_phasor[1]
reference_imag = calib_phasor[2]

calibrated_phasor = phasor_calibrate(real, imag, reference_mean, reference_real,\
	reference_imag, frequency, lifetime)


output = ij.py.to_dataset(np.array([calibrated_phasor[0], calibrated_phasor[1]], dtype=np.float32))
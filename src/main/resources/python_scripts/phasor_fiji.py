#@ ImageJ ij
#@ Img img

#@output Img output

import phasorpy
from phasorpy.phasor import phasor_from_signal
import numpy as np

data_arr = ij.py.to_xarray(img)

data_arr = data_arr.values.transpose(1, 2, 0).copy()
phasor = phasor_from_signal(data_arr)

output = ij.py.to_dataset(np.array([phasor[0], phasor[1], phasor[2]], dtype=np.float32))

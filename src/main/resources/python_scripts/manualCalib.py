#@ ImageJ ij
#@ double mod_factor
#@ double phase_shift
#@ Img raw_phasor
#@output Img output

import phasorpy
from phasorpy.phasor import phasor_transform
import numpy as np

raw_phasor_arr = ij.py.to_xarray(raw_phasor)
raw_phasor_arr = raw_phasor_arr.values.transpose(1, 2, 0).copy()

phasor = phasor_transform(raw_phasor_arr[:, :, 1], raw_phasor_arr[:, :, 2], phase_shift, mod_factor)

output = ij.py.to_dataset(np.array([phasor[0], phasor[1]], dtype=np.float32))
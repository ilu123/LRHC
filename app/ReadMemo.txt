byte[] to Mat:

        byte[] raw_data = ...;
        Mat mat = new Mat();
        mat.put(0, 0, raw_data);
Mat to byte[]:

        byte[] return_buff = new byte[(int) (result_mat.total() *
                                            result_mat.channels())];
        result_mat.get(0, 0, return_buff);
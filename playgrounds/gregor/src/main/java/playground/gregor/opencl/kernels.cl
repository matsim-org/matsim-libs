kernel void ExpVectorAdd(global const float* a, global const float* b, global float* c, int numElements) {
    int iGID = get_global_id(0);
    if (iGID >= numElements)  {
           return;
    }
    c[iGID] = exp(a[iGID])+exp(b[iGID]);
}
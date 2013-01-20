/*
 * BaseDataFile.h
 *
 *  Created on: Jul 8, 2012
 *      Author: pat2
 */

#ifndef BASEDATAFILE_H_
#define BASEDATAFILE_H_

using namespace std;


#include "LargeByteBuffer.h"
#include "ByteUtilities.h"
#include "Offset.h"

namespace TraceviewerServer
{

	class BaseDataFile
	{
	public:
		BaseDataFile(string, int);
		virtual ~BaseDataFile();
		int getNumberOfFiles();
		Offset getOffset(int);
		LargeByteBuffer* getMasterBuffer();
		void setData(string, int);

		bool IsMultiProcess();
		bool IsMultiThreading();
		bool IsHybrid();

		//string* ValuesX;
	  int* ProcessIDs;
		short* ThreadIDs;
	private:
		int Type; // = Constants::MULTI_PROCESSES | Constants::MULTI_THREADING;
		LargeByteBuffer* MasterBuff;
		int NumFiles; // = 0;

		Offset* Offsets;
		//Long* lOffsets;
	};

} /* namespace TraceviewerServer */
#endif /* BASEDATAFILE_H_ */
